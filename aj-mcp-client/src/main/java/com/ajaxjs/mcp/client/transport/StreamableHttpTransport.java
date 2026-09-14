package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializationNotification;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * MCP Streamable HTTP transport used by protocol revisions 2025-03-26 and
 * 2025-06-18. A single endpoint accepts every JSON-RPC POST; a response can be
 * ordinary JSON or an SSE stream. The optional GET stream carries unsolicited
 * server messages.
 */
@Slf4j
public class StreamableHttpTransport extends McpTransport {
    /**
     * Defines the protocol version header constant.
     */
    public static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    /**
     * Defines the session id header constant.
     */
    public static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    /**
     * Holds the endpoint url value.
     */
    private final String endpointUrl;
    /**
     * Holds the client value.
     */
    private final OkHttpClient client;
    /**
     * Holds the open event stream value.
     */
    private final boolean openEventStream;
    /**
     * Holds the request headers value.
     */
    private final Map<String, String> requestHeaders;
    /**
     * Holds the session id value.
     */
    private volatile String sessionId;
    /**
     * Holds the initialization version value.
     */
    private volatile String initializationVersion;
    /**
     * Holds the event source value.
     */
    private volatile EventSource eventSource;
    /**
     * Holds the closed value.
     */
    private volatile boolean closed;

    /** Completion of the first GET connection; independent of POST initialization. */
    private final CompletableFuture<Void> eventStreamReady = new CompletableFuture<>();
    /** Serializes bounded GET reconnection attempts. */
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "aj-mcp-http-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    /** Last successfully dispatched GET event, used only when resuming that stream. */
    private volatile String lastEventId;
    /** Whether the GET connection is currently open. */
    private volatile boolean eventStreamOpen;
    /** Most recent GET failure, observable without failing unrelated POST calls. */
    private volatile Throwable eventStreamFailure;
    /** Consecutive GET disconnect count, capped at five retries. */
    private int reconnectAttempts;
    /** Scheduled reconnect, cancelled during shutdown. */
    private ScheduledFuture<?> reconnectTask;

    /**
     * Creates a new streamable http transport.
     *
     * @param endpointUrl the endpoint url value.
     */
    public StreamableHttpTransport(String endpointUrl) {
        this(endpointUrl, true, Duration.ofSeconds(60), null);
    }

    /**
     * Creates a new streamable http transport.
     *
     * @param endpointUrl     the endpoint url value.
     * @param openEventStream the open event stream value.
     * @param timeout         the timeout value.
     * @param requestHeaders  the request headers value.
     */
    @Builder
    public StreamableHttpTransport(String endpointUrl, boolean openEventStream, Duration timeout,
                                   Map<String, String> requestHeaders) {
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "Missing MCP endpoint URL");
        this.openEventStream = openEventStream;
        this.requestHeaders = requestHeaders == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));
        Duration effectiveTimeout = timeout == null || timeout.isZero() ? Duration.ofSeconds(60) : timeout;
        if (effectiveTimeout.isNegative())
            throw new IllegalArgumentException("timeout must not be negative");
        this.client = new OkHttpClient.Builder()
                .callTimeout(effectiveTimeout)
                .connectTimeout(effectiveTimeout)
                // SSE GET streams are intentionally long-lived.
                .readTimeout(Duration.ZERO)
                .writeTimeout(effectiveTimeout)
                .build();
    }

    @Override
    public void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
        if (closed)
            throw new IllegalStateException("Streamable HTTP transport is closed");
        setPendingRequests(pendingRequest);
    }

    @Override
    public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
        initializationVersion = request.getParams().getProtocolVersion();
        CompletableFuture<JsonNode> response = completeInitialization(
                post(request, numericId(request.getId()), false),
                () -> post(new InitializationNotification(), null, true));

        if (!openEventStream) {
            eventStreamReady.completeExceptionally(new IllegalStateException("GET event stream is disabled"));
            return response;
        }

        return response.thenApply(result -> {
            openGetStream();
            return result;
        });
    }

    @Override
    public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
        requireInitialized();

        return post(request, numericId(request.getId()), true);
    }

    @Override
    public void sendRequestWithoutResponse(McpRequest request) {
        post(request, null, true);
    }

    @Override
    protected void sendJson(JsonNode message) {
        postJson(message, null, true);
    }

    /**
     * Executes the post operation.
     *
     * @param message       the message value.
     * @param id            the id value.
     * @param versionHeader the version header value.
     * @return the result of the post operation.
     */
    private CompletableFuture<JsonNode> post(BaseJsonRpcMessage message, Long id, boolean versionHeader) {
        try {
            return postBytes(JsonUtils.toJsonBytes(message), id, versionHeader);
        } catch (IOException e) {
            return McpUtils.failedFuture(e);
        }
    }

    /**
     * Executes the post json operation.
     *
     * @param message       the message value.
     * @param id            the id value.
     * @param versionHeader the version header value.
     * @return the result of the post json operation.
     */
    private CompletableFuture<JsonNode> postJson(JsonNode message, Long id, boolean versionHeader) {
        try {
            return postBytes(JsonUtils.toJsonBytes(message), id, versionHeader);
        } catch (IOException e) {
            return McpUtils.failedFuture(e);
        }
    }

    /**
     * Executes the post bytes operation.
     *
     * @param json          the json value.
     * @param id            the id value.
     * @param versionHeader the version header value.
     * @return the result of the post bytes operation.
     */
    private CompletableFuture<JsonNode> postBytes(byte[] json, Long id, boolean versionHeader) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();

        if (closed) {
            future.completeExceptionally(new IOException("Streamable HTTP transport is closed"));
            return future;
        }

        if (id != null)
            saveRequest(id, future);

        Request.Builder builder = baseRequest(versionHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .post(RequestBody.create(json));

        synchronized (this) {
            if (closed) {
                future.completeExceptionally(new IOException("Streamable HTTP transport is closed"));
                return future;
            }
            Call pendingCall = client.newCall(builder.build());
            future.whenComplete((result, failure) -> {
                if (future.isCancelled())
                    pendingCall.cancel();
            });
            pendingCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (id != null)
                        failOne(id, future, e);
                    else
                        future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (Response ignored = response) {
                        captureSession(response);

                        if (!response.isSuccessful()) {
                            String body = response.body() == null ? "" : response.body().string();
                            failOne(id, future, new IOException("MCP HTTP " + response.code() + ": " + body));
                            return;
                        }

                        if (response.code() == 202 || response.code() == 204 || response.body() == null) {
                            if (id == null)
                                future.complete(null);
                            else
                                failOne(id, future, new IOException("Missing JSON-RPC response for request " + id));
                            return;
                        }

                        String contentType = response.header("Content-Type", "");

                        assert contentType != null;
                        if (contentType.startsWith("text/event-stream"))
                            handleSsePayload(response.body(), future);
                        else {
                            String body = response.body().string();
                            if (!body.trim().isEmpty())
                                handle(JsonUtils.json2Node(body));
                        }

                        if (!future.isDone()) {
                            if (id == null)
                                future.complete(null);
                            else
                                failOne(id, future, new IOException("Response ended before request " + id + " completed"));
                        }
                    } catch (Exception e) {
                        failOne(id, future, e);
                    }
                }
            });
        }
        return future;
    }

    /**
     * Executes the base request operation.
     *
     * @param includeVersion the include version value.
     * @return the result of the base request operation.
     */
    private Request.Builder baseRequest(boolean includeVersion) {
        Request.Builder builder = new Request.Builder().url(endpointUrl);
        // Authorization (for example Bearer tokens) remains application-owned.
        // The transport only carries explicitly configured headers.
        for (Map.Entry<String, String> header : requestHeaders.entrySet())
            builder.header(header.getKey(), header.getValue());

        if (sessionId != null)
            builder.header(SESSION_ID_HEADER, sessionId);

        String version = getNegotiatedProtocolVersion();

        if (version == null)
            version = initializationVersion;

        if (includeVersion && version != null)
            builder.header(PROTOCOL_VERSION_HEADER, version);

        return builder;
    }

    /**
     * Executes the capture session operation.
     *
     * @param response the response value.
     */
    private void captureSession(Response response) {
        String received = response.header(SESSION_ID_HEADER);

        if (received != null && !received.trim().isEmpty())
            sessionId = received;
    }

    /**
     * Executes the fail one operation.
     *
     * @param id      the id value.
     * @param future  the future value.
     * @param failure the failure value.
     */
    private void failOne(Long id, CompletableFuture<JsonNode> future, Throwable failure) {
        future.completeExceptionally(failure);
    }

    /**
     * Parses all data records in an SSE response while preserving record boundaries.
     * @param body streaming response body, closed by the caller
     * @param future pending response; parsing stops when it completes
     * @throws IOException if the stream cannot be read
     */
    private void handleSsePayload(ResponseBody body, CompletableFuture<JsonNode> future) throws IOException {
        StringBuilder data = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8));
        String line;
        while (!future.isDone() && (line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                dispatchSseData(data);
                data.setLength(0);
            } else if (line.startsWith("data:")) {
                if (data.length() > 0)
                    data.append('\n');

                String value = line.substring(5);
                data.append(value.startsWith(" ") ? value.substring(1) : value);
            }
        }

        // An incomplete event at EOF is not dispatched; SSE records end at a blank line.
    }

    /**
     * Executes the dispatch sse data operation.
     *
     * @param data the data value.
     */
    private void dispatchSseData(StringBuilder data) {
        if (data.length() > 0)
            handle(JsonUtils.json2Node(data.toString()));
    }

    /**
     * Executes the open get stream operation.
     */
    private synchronized void openGetStream() {
        if (closed)
            return;
        Request.Builder builder = baseRequest(true).header("Accept", "text/event-stream").get();
        if (lastEventId != null)
            builder.header("Last-Event-ID", lastEventId);
        Request request = builder.build();
        eventSource = EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(EventSource source, Response response) {
                synchronized (StreamableHttpTransport.this) {
                    if (closed || source != eventSource) {
                        source.cancel();
                        return;
                    }
                    eventStreamOpen = true;
                    eventStreamFailure = null;
                    eventStreamReady.complete(null);
                }
            }

            @Override
            public void onEvent(EventSource source, String id, String type, String data) {
                try {
                    if (data != null && !data.trim().isEmpty())
                        handle(JsonUtils.json2Node(data));
                    synchronized (StreamableHttpTransport.this) {
                        if (source != eventSource || closed)
                            return;
                        if (id != null)
                            lastEventId = id;
                        reconnectAttempts = 0;
                    }
                } catch (RuntimeException e) {
                    source.cancel();
                    getStreamFailed(source, e, null);
                }
            }

            @Override
            public void onFailure(EventSource source, Throwable t, Response response) {
                getStreamFailed(source, t == null ? new IOException("MCP event stream closed") : t, response);
            }

            @Override
            public void onClosed(EventSource source) {
                getStreamFailed(source, new IOException("MCP event stream closed"), null);
            }
        });
    }

    /**
     * Records a GET failure and schedules at most five exponential-backoff retries.
     * @param source disconnected source, used to ignore obsolete callbacks
     * @param failure cause exposed through the GET health API
     * @param response optional HTTP error response
     */
    private synchronized void getStreamFailed(EventSource source, Throwable failure, Response response) {
        if (closed || source != eventSource)
            return;
        eventSource = null;
        eventStreamOpen = false;
        eventStreamFailure = failure;
        int status = response == null ? 0 : response.code();
        if ((status >= 400 && status < 500) || reconnectAttempts >= 5) {
            eventStreamReady.completeExceptionally(failure);
            log.warn("MCP GET stream unavailable (HTTP {}); POST requests remain independent", status);
            return;
        }
        long delay = Math.min(5000L, 200L << reconnectAttempts++);
        reconnectTask = reconnectExecutor.schedule(this::openGetStream, delay, TimeUnit.MILLISECONDS);
    }

    /** @return a future for the first GET connection; callers may apply their own readiness timeout. */
    public CompletableFuture<Void> getEventStreamReady() {
        return eventStreamReady.thenApply(ignored -> null);
    }

    /** @return whether the optional GET stream is connected now. */
    public boolean isEventStreamOpen() {
        return eventStreamOpen;
    }

    /** @return the latest GET failure, or null while connected. */
    public Throwable getEventStreamFailure() {
        return eventStreamFailure;
    }

    @Override
    public void checkHealth() {
        if (closed)
            throw new IllegalStateException("Streamable HTTP transport is closed");
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed)
                return;
            closed = true;
            eventStreamOpen = false;
            if (reconnectTask != null)
                reconnectTask.cancel(false);
            if (eventSource != null)
                eventSource.cancel();
        }
        reconnectExecutor.shutdownNow();
        eventStreamReady.completeExceptionally(new IOException("Streamable HTTP transport is closed"));
        failPendingRequests(new IOException("Streamable HTTP transport is closed"));

        if (sessionId != null) {
            // A short synchronous deadline keeps shutdown bounded even if DELETE is unsupported.
            OkHttpClient closingClient = client.newBuilder().callTimeout(Duration.ofSeconds(2)).build();
            try (Response ignored = closingClient.newCall(baseRequest(true).delete().build()).execute()) {
                // Session termination is best-effort; local resources must always be released.
            } catch (IOException | RuntimeException e) {
                log.debug("Unable to terminate MCP HTTP session", e);
            }
        }

        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    /**
     * Executes the get session id operation.
     *
     * @return the result of the get session id operation.
     */
    public String getSessionId() {
        return sessionId;
    }
}
