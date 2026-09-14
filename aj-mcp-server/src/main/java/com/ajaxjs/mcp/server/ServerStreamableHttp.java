package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.ProtocolVersion;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Servlet-framework-neutral server adapter for MCP Streamable HTTP.
 *
 * <p>Controllers pass request headers/body to {@link #post(String, Map)} and copy
 * the returned status, headers and body to their HTTP response. GET event streams
 * are registered through {@link #openEventStream(String, PrintWriter, Map)}.</p>
 */
@Slf4j
public class ServerStreamableHttp implements McpTransportSync {
    /**
     * Defines the protocol version header constant.
     */
    public static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    /**
     * Defines the session id header constant.
     */
    public static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    /**
     * Holds the server value.
     */
    private final McpServer server;
    /**
     * Holds the streams value.
     */
    private final Map<String, StreamSession> streams = new ConcurrentHashMap<>();
    /**
     * Holds the closed value.
     */
    private volatile boolean closed;

    /** All initialized HTTP sessions, independently of optional GET streams. */
    private final Map<String, SessionActivity> sessions = new ConcurrentHashMap<>();
    /** Owns periodic heartbeat and idle-expiration work. */
    private ScheduledExecutorService maintenance;

    /** Tracks activity with a monotonic clock; in-flight POSTs never expire. */
    private static final class SessionActivity {
        /** Last observed activity in nanoseconds. */
        private volatile long lastActivity = System.nanoTime();
        /** Active POST count, guarded by the transport monitor. */
        private int activePosts;
    }

    /**
     * Creates a new server streamable http.
     *
     * @param server the server value.
     */
    public ServerStreamableHttp(McpServer server) {
        this.server = server;
    }

    /**
     * Processes one Streamable HTTP POST request. JSON-RPC batching is intentionally unsupported.
     *
     * @param body    the request body containing one JSON-RPC message.
     * @param headers the HTTP request headers.
     * @return the HTTP response to send to the client.
     */
    public HttpResult post(String body, Map<String, String> headers) {
        if (closed)
            return HttpResult.text(503, "MCP transport is closed");
        HttpResult originFailure = validateOrigin(headers);

        if (originFailure != null)
            return originFailure;

        String contentType = header(headers, "Content-Type");

        if (contentType != null && !contentType.toLowerCase().startsWith("application/json"))
            return HttpResult.text(415, "Content-Type must be application/json");

        JsonNode envelope;

        try {
            envelope = JsonUtils.json2Node(body);
        } catch (RuntimeException e) {
            return HttpResult.json(400, new JsonRpcErrorException(JsonRpcErrorCode.PARSE_ERROR,
                    "Unable to parse the JSON message").toJson());
        }

        if (envelope == null || !envelope.isObject())
            return HttpResult.json(400, new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST,
                    "JSON-RPC batching is not supported").toJson());

        String method = envelope.path("method").asText(null);
        boolean initializing = McpConstant.Methods.INITIALIZE.equals(method);
        String sessionId = header(headers, SESSION_ID_HEADER);

        if (initializing) {
            if (sessionId != null)
                return HttpResult.text(400, "Initialization must not include an MCP session id");
            sessionId = UUID.randomUUID().toString();
        } else {
            if (sessionId == null)
                return HttpResult.text(400, "Missing " + SESSION_ID_HEADER);
            String negotiated = server.getNegotiatedProtocolVersion(sessionId);

            if (negotiated == null)
                return HttpResult.text(404, "Unknown or expired MCP session");

            String suppliedVersion = header(headers, PROTOCOL_VERSION_HEADER);
            // 2025-06-18 made the negotiated HTTP version header mandatory.

            if (ProtocolVersion.V_2025_06_18.value().equals(negotiated)
                    && !negotiated.equals(suppliedVersion))
                return HttpResult.text(400, "Missing or invalid " + PROTOCOL_VERSION_HEADER);

            if (suppliedVersion != null && !negotiated.equals(suppliedVersion))
                return HttpResult.text(400, "Protocol version does not match the initialized session");
        }

        boolean notification = !envelope.has("id") && envelope.path("method").isTextual()
                && "2.0".equals(envelope.path("jsonrpc").textValue());
        synchronized (this) {
            if (closed)
                return HttpResult.text(503, "MCP transport is closed");
            if (initializing)
                sessions.put(sessionId, new SessionActivity());
            SessionActivity activity = sessions.get(sessionId);
            if (activity == null)
                return HttpResult.text(404, "Unknown or expired MCP session");
            activity.activePosts++;
            activity.lastActivity = System.nanoTime();
            start();
        }
        try {
            server.bindSession(sessionId);
            if (server.acceptClientResponse(sessionId, body))
                return new HttpResult(202, Collections.emptyMap(), null, null);
            McpRequestRawInfo raw = McpServerInitialize.jsonRpcValidate(body);
            McpResponse response = server.processMessage(raw);
            Map<String, String> responseHeaders = initializing
                    ? Collections.singletonMap(SESSION_ID_HEADER, sessionId) : Collections.emptyMap();

            return response == null
                    ? new HttpResult(202, responseHeaders, null, null)
                    : new HttpResult(200, responseHeaders, "application/json", JsonUtils.toJson(response));
        } catch (JsonRpcErrorException e) {
            if (notification)
                return new HttpResult(202, Collections.emptyMap(), null, null);
            return HttpResult.json(200, e.toJson());
        } catch (RuntimeException e) {
            log.warn("MCP HTTP message processing failed", e);
            if (notification)
                return new HttpResult(202, Collections.emptyMap(), null, null);
            Object id = envelope.has("id") ? JsonUtils.convertValue(envelope.get("id"), Object.class) : null;
            return HttpResult.json(200, new JsonRpcErrorException(id, JsonRpcErrorCode.INTERNAL_ERROR,
                    "Internal error").toJson());
        } finally {
            server.clearSession();
            synchronized (this) {
                SessionActivity activity = sessions.get(sessionId);
                if (activity != null) {
                    activity.activePosts--;
                    activity.lastActivity = System.nanoTime();
                }
                if (closed || activity == null || server.getNegotiatedProtocolVersion(sessionId) == null)
                    removeSession(sessionId);
            }
        }
    }

    /**
     * Registers the optional long-lived GET stream used for server-originated messages.
     *
     * @param sessionId the initialized MCP session identifier.
     * @param writer    the response writer for the event stream.
     * @param headers   the HTTP request headers.
     * @return the HTTP response that opens or rejects the event stream.
     */
    public synchronized HttpResult openEventStream(String sessionId, PrintWriter writer, Map<String, String> headers) {
        if (closed)
            return HttpResult.text(503, "MCP transport is closed");
        HttpResult originFailure = validateOrigin(headers);

        if (originFailure != null)
            return originFailure;

        if (sessionId == null || !sessions.containsKey(sessionId) || server.getNegotiatedProtocolVersion(sessionId) == null)
            return HttpResult.text(404, "Unknown or expired MCP session");

        String negotiated = server.getNegotiatedProtocolVersion(sessionId);
        String suppliedVersion = header(headers, PROTOCOL_VERSION_HEADER);

        if (ProtocolVersion.V_2025_06_18.value().equals(negotiated) && !negotiated.equals(suppliedVersion))
            return HttpResult.text(400, "Missing or invalid " + PROTOCOL_VERSION_HEADER);

        if (suppliedVersion != null && !negotiated.equals(suppliedVersion))
            return HttpResult.text(400, "Protocol version does not match the initialized session");

        StreamSession previous = streams.put(sessionId, new StreamSession(writer));
        sessions.get(sessionId).lastActivity = System.nanoTime();

        if (previous != null)
            previous.close();

        return new HttpResult(200, Collections.<String, String>emptyMap(), "text/event-stream", null);
    }

    /**
     * Executes the delete operation.
     *
     * @param sessionId the session id value.
     * @param headers   the headers value.
     * @return the result of the delete operation.
     */
    public HttpResult delete(String sessionId, Map<String, String> headers) {
        HttpResult originFailure = validateOrigin(headers);

        if (originFailure != null)
            return originFailure;

        if (sessionId == null || server.getNegotiatedProtocolVersion(sessionId) == null)
            return HttpResult.text(404, "Unknown or expired MCP session");

        removeSession(sessionId);

        return new HttpResult(204, Collections.<String, String>emptyMap(), null, null);
    }

    /**
     * Executes the validate origin operation.
     *
     * @param headers the headers value.
     * @return the result of the validate origin operation.
     */
    private HttpResult validateOrigin(Map<String, String> headers) {
        String origin = header(headers, "Origin");

        if (origin != null && (server.getServerConfig() == null
                || !server.getServerConfig().getAllowedOrigins().contains(origin)))
            return HttpResult.text(403, "Forbidden Origin");

        return null;
    }

    /**
     * Executes the header operation.
     *
     * @param headers the headers value.
     * @param name    the name value.
     * @return the result of the header operation.
     */
    private static String header(Map<String, String> headers, String name) {
        if (headers == null)
            return null;

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey()))
                return entry.getValue();
        }

        return null;
    }

    @Override
    public void send(String sessionId, String json) {
        StreamSession stream = streams.get(sessionId);

        if (stream == null)
            throw new IllegalStateException("No Streamable HTTP GET stream for session " + sessionId);
        try {
            stream.send(json);
        } catch (RuntimeException e) {
            closeEventStream(sessionId, stream.writer);
            throw e;
        }
    }

    @Override
    public void broadcast(String json) {
        for (String sessionId : streams.keySet()) {
            try {
                send(sessionId, json);
            } catch (RuntimeException e) {
                log.debug("Removing failed GET stream for {}", sessionId, e);
            }
        }
    }

    /**
     * Detaches only this writer when its owning HTTP response completes, times out or disconnects.
     * A replacement GET stream is not affected. The session remains available for reconnection.
     * @param sessionId initialized session identifier
     * @param writer writer belonging to the completed response
     */
    public synchronized void closeEventStream(String sessionId, PrintWriter writer) {
        StreamSession stream = streams.get(sessionId);
        if (stream != null && stream.writer == writer && streams.remove(sessionId, stream)) {
            stream.close();
            server.failClientRequests(sessionId, new IllegalStateException("MCP GET stream disconnected"));
        }
    }

    /**
     * Executes the remove session operation.
     *
     * @param sessionId the session id value.
     */
    private synchronized void removeSession(String sessionId) {
        sessions.remove(sessionId);
        StreamSession stream = streams.remove(sessionId);

        if (stream != null)
            stream.close();

        server.removeSession(sessionId);
    }

    @Override
    public synchronized void start() {
        if (closed) throw new IllegalStateException("Transport is closed");
        if (maintenance == null) {
            maintenance = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "aj-mcp-http-maintenance");
                thread.setDaemon(true);
                return thread;
            });
            maintenance.scheduleWithFixedDelay(() -> maintainSessions(System.nanoTime()), 15, 15, TimeUnit.SECONDS);
        }
    }

    /**
     * Sends heartbeats and expires inactive sessions using a monotonic timestamp.
     * @param now current monotonic time in nanoseconds
     */
    synchronized void maintainSessions(long now) {
        if (closed)
            return;
        Duration configured = server.getServerConfig() == null ? null : server.getServerConfig().getSessionIdleTimeout();
        long idleNanos = configured == null || configured.isZero() || configured.isNegative()
                ? Duration.ofMinutes(30).toNanos() : configured.toNanos();
        for (Map.Entry<String, SessionActivity> entry : sessions.entrySet()) {
            SessionActivity activity = entry.getValue();
            if (activity.activePosts == 0 && now - activity.lastActivity >= idleNanos) {
                removeSession(entry.getKey());
                continue;
            }
            StreamSession stream = streams.get(entry.getKey());
            if (stream != null) {
                try {
                    stream.frame(": heartbeat\n\n");
                } catch (RuntimeException e) {
                    closeEventStream(entry.getKey(), stream.writer);
                }
            }
        }
    }

    @Override
    public void initialize() {
        start();
    }

    @Override
    public String handle(String rawJson) {
        HttpResult result = post(rawJson, Collections.<String, String>emptyMap());

        return result.getBody();
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed)
            return;

        closed = true;
        if (maintenance != null)
            maintenance.shutdownNow();
        for (String sessionId : new ArrayList<>(sessions.keySet()))
            removeSession(sessionId);
    }

    /**
     * Represents http result.
     */
    @Data
    @AllArgsConstructor
    public static class HttpResult {
        /**
         * Holds the status value.
         */
        private int status;
        /**
         * Holds the headers value.
         */
        private Map<String, String> headers;
        /**
         * Holds the content type value.
         */
        private String contentType;
        /**
         * Holds the body value.
         */
        private String body;

        /**
         * Executes the json operation.
         *
         * @param status the status value.
         * @param body   the body value.
         * @return the result of the json operation.
         */
        static HttpResult json(int status, String body) {
            return new HttpResult(status, Collections.<String, String>emptyMap(), "application/json", body);
        }

        /**
         * Executes the text operation.
         *
         * @param status the status value.
         * @param body   the body value.
         * @return the result of the text operation.
         */
        static HttpResult text(int status, String body) {
            return new HttpResult(status, Collections.<String, String>emptyMap(), "text/plain", body);
        }
    }

    /**
     * Represents stream session.
     */
    private static final class StreamSession {
        /**
         * Holds the writer value.
         */
        private final PrintWriter writer;

        /**
         * Creates a new stream session.
         *
         * @param writer the writer value.
         */
        private StreamSession(PrintWriter writer) {
            if (writer == null)
                throw new IllegalArgumentException("writer is required");

            this.writer = writer;
        }

        /**
         * Executes the send operation.
         *
         * @param json the json value.
         */
        private void send(String json) {
            frame("event: message\ndata: " + json + "\n\n");
        }

        /**
         * Writes one complete SSE frame and detects suppressed writer errors.
         * @param frame complete SSE frame including its trailing blank line
         */
        private void frame(String frame) {
            synchronized (writer) {
                writer.write(frame);
                writer.flush();

                if (writer.checkError())
                    throw new IllegalStateException("Streamable HTTP SSE write failed");
            }
        }

        /**
         * Executes the close operation.
         */
        private void close() {
            synchronized (writer) {
                writer.close();
            }
        }
    }
}
