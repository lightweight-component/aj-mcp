package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.ProtocolVersion;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet-framework-neutral server adapter for MCP Streamable HTTP.
 *
 * <p>Controllers pass request headers/body to {@link #post(String, Map)} and copy
 * the returned status, headers and body to their HTTP response. GET event streams
 * are registered through {@link #openEventStream(String, PrintWriter, Map)}.</p>
 */
public class ServerStreamableHttp implements McpTransportSync {
    public static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    public static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    private final McpServer server;
    private final Map<String, StreamSession> streams = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public ServerStreamableHttp(McpServer server) {
        this.server = server;
    }

    /** Processes one Streamable HTTP POST request. JSON-RPC batching is intentionally unsupported. */
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
        if (envelope.isArray())
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

        try {
            server.bindSession(sessionId);
            McpRequestRawInfo raw = McpServerInitialize.jsonRpcValidate(body);
            McpResponse response = server.processMessage(raw);
            Map<String, String> responseHeaders = initializing
                    ? Collections.singletonMap(SESSION_ID_HEADER, sessionId) : Collections.emptyMap();
            return response == null
                    ? new HttpResult(202, responseHeaders, null, null)
                    : new HttpResult(200, responseHeaders, "application/json", JsonUtils.toJson(response));
        } catch (JsonRpcErrorException e) {
            return HttpResult.json(200, e.toJson());
        } catch (RuntimeException e) {
            return HttpResult.json(200, new JsonRpcErrorException(JsonRpcErrorCode.INTERNAL_ERROR,
                    e.getMessage() == null ? "Internal error" : e.getMessage()).toJson());
        } finally {
            server.clearSession();
        }
    }

    /** Registers the optional long-lived GET stream used for server-originated messages. */
    public HttpResult openEventStream(String sessionId, PrintWriter writer, Map<String, String> headers) {
        HttpResult originFailure = validateOrigin(headers);
        if (originFailure != null)
            return originFailure;
        if (sessionId == null || server.getNegotiatedProtocolVersion(sessionId) == null)
            return HttpResult.text(404, "Unknown or expired MCP session");
        String negotiated = server.getNegotiatedProtocolVersion(sessionId);
        String suppliedVersion = header(headers, PROTOCOL_VERSION_HEADER);
        if (ProtocolVersion.V_2025_06_18.value().equals(negotiated)
                && !negotiated.equals(suppliedVersion))
            return HttpResult.text(400, "Missing or invalid " + PROTOCOL_VERSION_HEADER);
        if (suppliedVersion != null && !negotiated.equals(suppliedVersion))
            return HttpResult.text(400, "Protocol version does not match the initialized session");
        StreamSession previous = streams.put(sessionId, new StreamSession(writer));
        if (previous != null)
            previous.close();
        return new HttpResult(200, Collections.<String, String>emptyMap(), "text/event-stream", null);
    }

    public HttpResult delete(String sessionId, Map<String, String> headers) {
        HttpResult originFailure = validateOrigin(headers);
        if (originFailure != null)
            return originFailure;
        if (sessionId == null || server.getNegotiatedProtocolVersion(sessionId) == null)
            return HttpResult.text(404, "Unknown or expired MCP session");
        removeSession(sessionId);
        return new HttpResult(204, Collections.<String, String>emptyMap(), null, null);
    }

    private HttpResult validateOrigin(Map<String, String> headers) {
        String origin = header(headers, "Origin");
        if (origin != null && (server.getServerConfig() == null
                || !server.getServerConfig().getAllowedOrigins().contains(origin)))
            return HttpResult.text(403, "Forbidden Origin");
        return null;
    }

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
            if (streams.remove(sessionId, stream))
                stream.close();
            server.removeSession(sessionId);
            throw e;
        }
    }

    @Override
    public void broadcast(String json) {
        for (String sessionId : streams.keySet())
            send(sessionId, json);
    }

    private void removeSession(String sessionId) {
        StreamSession stream = streams.remove(sessionId);
        if (stream != null)
            stream.close();
        server.removeSession(sessionId);
    }

    @Override public void start() { if (closed) throw new IllegalStateException("Transport is closed"); }
    @Override public void initialize() { start(); }

    @Override
    public String handle(String rawJson) {
        HttpResult result = post(rawJson, Collections.<String, String>emptyMap());
        return result.getBody();
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;
        closed = true;
        for (String sessionId : streams.keySet())
            removeSession(sessionId);
    }

    @Data
    @AllArgsConstructor
    public static class HttpResult {
        private int status;
        private Map<String, String> headers;
        private String contentType;
        private String body;

        static HttpResult json(int status, String body) {
            return new HttpResult(status, Collections.<String, String>emptyMap(), "application/json", body);
        }

        static HttpResult text(int status, String body) {
            return new HttpResult(status, Collections.<String, String>emptyMap(), "text/plain", body);
        }
    }

    private static final class StreamSession {
        private final PrintWriter writer;

        private StreamSession(PrintWriter writer) {
            if (writer == null)
                throw new IllegalArgumentException("writer is required");
            this.writer = writer;
        }

        private void send(String json) {
            synchronized (writer) {
                writer.write("event: message\ndata: " + json + "\n\n");
                writer.flush();
                if (writer.checkError())
                    throw new IllegalStateException("Streamable HTTP SSE write failed");
            }
        }

        private void close() {
            synchronized (writer) {
                writer.close();
            }
        }
    }
}
