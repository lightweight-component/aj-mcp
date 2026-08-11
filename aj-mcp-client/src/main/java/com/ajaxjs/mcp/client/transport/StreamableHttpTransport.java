package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializationNotification;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * MCP Streamable HTTP transport used by protocol revisions 2025-03-26 and
 * 2025-06-18. A single endpoint accepts every JSON-RPC POST; a response can be
 * ordinary JSON or an SSE stream. The optional GET stream carries unsolicited
 * server messages.
 */
public class StreamableHttpTransport extends McpTransport {
    public static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    public static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    private final String endpointUrl;
    private final OkHttpClient client;
    private final boolean openEventStream;
    private final Map<String, String> requestHeaders;
    private volatile String sessionId;
    private volatile String initializationVersion;
    private volatile EventSource eventSource;
    private volatile boolean closed;

    public StreamableHttpTransport(String endpointUrl) {
        this(endpointUrl, true, Duration.ofSeconds(60), null);
    }

    @Builder
    public StreamableHttpTransport(String endpointUrl, boolean openEventStream, Duration timeout,
                                   Map<String, String> requestHeaders) {
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "Missing MCP endpoint URL");
        this.openEventStream = openEventStream;
        this.requestHeaders = requestHeaders == null ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));
        Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(60) : timeout;
        this.client = new OkHttpClient.Builder()
                .callTimeout(effectiveTimeout)
                .connectTimeout(effectiveTimeout)
                // SSE GET streams are intentionally long lived.
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
        return post(request, numericId(request.getId()), false).thenCompose(response -> {
            InitializationNotification initialized = new InitializationNotification();
            return post(initialized, null, true).thenApply(ignored -> {
                if (openEventStream)
                    openGetStream();
                return response;
            });
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

    private CompletableFuture<JsonNode> post(BaseJsonRpcMessage message, Long id, boolean versionHeader) {
        try {
            return postBytes(JsonUtils.OBJECT_MAPPER.writeValueAsBytes(message), id, versionHeader);
        } catch (IOException e) {
            return McpUtils.failedFuture(e);
        }
    }

    private CompletableFuture<JsonNode> postJson(JsonNode message, Long id, boolean versionHeader) {
        try {
            return postBytes(JsonUtils.OBJECT_MAPPER.writeValueAsBytes(message), id, versionHeader);
        } catch (IOException e) {
            return McpUtils.failedFuture(e);
        }
    }

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
        client.newCall(builder.build()).enqueue(new Callback() {
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
                        future.complete(null);
                        return;
                    }
                    String body = response.body().string();
                    String contentType = response.header("Content-Type", "");
                    if (contentType.startsWith("text/event-stream"))
                        handleSsePayload(body);
                    else if (!body.trim().isEmpty())
                        handle(JsonUtils.json2Node(body));
                    if (id == null && !future.isDone())
                        future.complete(null);
                } catch (Exception e) {
                    failOne(id, future, e);
                }
            }
        });
        return future;
    }

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

    private void captureSession(Response response) {
        String received = response.header(SESSION_ID_HEADER);
        if (received != null && !received.trim().isEmpty())
            sessionId = received;
    }

    private void failOne(Long id, CompletableFuture<JsonNode> future, Throwable failure) {
        if (id != null)
            future.completeExceptionally(failure);
        else
            future.completeExceptionally(failure);
    }

    /** Parses all data records in an SSE response while preserving record boundaries. */
    private void handleSsePayload(String payload) {
        StringBuilder data = new StringBuilder();
        for (String line : payload.split("\\r?\\n", -1)) {
            if (line.isEmpty()) {
                dispatchSseData(data);
                data.setLength(0);
            } else if (line.startsWith("data:")) {
                if (data.length() > 0)
                    data.append('\n');
                data.append(line.substring(5).trim());
            }
        }
        dispatchSseData(data);
    }

    private void dispatchSseData(StringBuilder data) {
        if (data.length() > 0)
            handle(JsonUtils.json2Node(data.toString()));
    }

    private void openGetStream() {
        Request request = baseRequest(true).header("Accept", "text/event-stream").get().build();
        eventSource = EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(EventSource source, String id, String type, String data) {
                if (data != null && !data.trim().isEmpty())
                    handle(JsonUtils.json2Node(data));
            }

            @Override
            public void onFailure(EventSource source, Throwable t, Response response) {
                if (!closed)
                    failPendingRequests(t == null ? new IOException("MCP event stream closed") : t);
            }
        });
    }

    @Override
    public void checkHealth() {
        if (closed)
            throw new IllegalStateException("Streamable HTTP transport is closed");
    }

    @Override
    public void close() {
        if (closed)
            return;
        closed = true;
        failPendingRequests(new IOException("Streamable HTTP transport is closed"));
        if (eventSource != null)
            eventSource.cancel();
        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    public String getSessionId() {
        return sessionId;
    }
}
