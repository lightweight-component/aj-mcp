package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializationNotification;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import okio.Buffer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Legacy MCP transport using an SSE notification channel and HTTP POST requests.
 *
 * <p>The server first opens the SSE channel and announces the POST endpoint.
 * Requests are then sent to that endpoint while responses and unsolicited
 * messages are correlated through the shared JSON-RPC request table. This class
 * is retained for servers implementing the older HTTP/SSE arrangement; new
 * deployments should prefer {@link StreamableHttpTransport} or
 * {@link AutoHttpTransport}.</p>
 *
 * <p>Closing is idempotent and fails pending requests. The supplied headers are
 * copied at construction time so later caller-side map changes do not alter an
 * active connection.</p>
 */
@Slf4j
public class HttpMcpTransport extends McpTransport {
    /**
     * The URL for the SSE (Server-Sent Events) connection.
     */
    private final String sseUrl;

    /**
     * The HTTP client used for making requests.
     */
    private final OkHttpClient client;

    /**
     * Flag indicating whether to log server responses.
     */
    private final boolean logResponses;

    /**
     * Flag indicating whether to log client requests.
     */
    private boolean logRequests;

    /**
     * The event listener for SSE events.
     */
    private EventSource mcpSseEventListener;

    /**
     * The URL for posting messages to the server.
     * This is obtained from the server after initializing the SSE channel.
     */
    private volatile String postUrl;

    /**
     * Holds the closed value.
     */
    private volatile boolean closed;

    private final Map<String, String> requestHeaders;
    private volatile CompletableFuture<String> endpointReady;

    /**
     * Creates a legacy HTTP/SSE transport with default timeouts and logging disabled.
     *
     * @param sseUrl SSE endpoint URL
     */
    public HttpMcpTransport(String sseUrl) {
        this(sseUrl, false, false);
    }

    /**
     * Creates a legacy HTTP/SSE transport.
     *
     * @param sseUrl       SSE endpoint URL
     * @param logResponses whether response bodies should be logged
     * @param logRequests  whether request bodies should be logged
     */
    @Builder
    public HttpMcpTransport(String sseUrl, boolean logResponses, boolean logRequests) {
        this(sseUrl, logResponses, logRequests, Duration.ofSeconds(60), Collections.emptyMap());
    }

    /**
     * Creates a configured legacy transport used by automatic HTTP discovery.
     *
     * @param sseUrl         the SSE endpoint URL
     * @param logResponses   whether responses should be logged
     * @param logRequests    whether requests should be logged
     * @param timeout        the HTTP operation timeout; null or zero uses the default
     * @param requestHeaders HTTP headers to send, or null for no additional headers
     */
    public HttpMcpTransport(String sseUrl, boolean logResponses, boolean logRequests,
                            Duration timeout, Map<String, String> requestHeaders) {
        Objects.requireNonNull(sseUrl, "Missing SSE endpoint URL");
        this.sseUrl = sseUrl;
        this.logRequests = logRequests;
        this.logResponses = logResponses;
        this.requestHeaders = requestHeaders == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        timeout = timeout == null || timeout.isZero() ? Duration.ofSeconds(60) : timeout;
        if (timeout.isNegative())
            throw new IllegalArgumentException("timeout must not be negative");
        httpClientBuilder.callTimeout(timeout);
        httpClientBuilder.connectTimeout(timeout);
        httpClientBuilder.readTimeout(Duration.ZERO);
        httpClientBuilder.writeTimeout(timeout);

        if (logRequests)
            httpClientBuilder.addInterceptor(chain -> {
                Request request = chain.request();
                String body;
                Buffer buffer = new Buffer();

                try {
                    if (request.body() == null)
                        body = McpConstant.EMPTY_STR;
                    else {
                        request.body().writeTo(buffer);
                        body = buffer.readUtf8();
                    }

                    log.debug("Request:\n- method: {}\n- url: {}\n- headers: {}\n- body: {}", request.method(), request.url(), getHeaders(request.headers()), body);
                } catch (Exception e) {
                    log.warn("Error while logging request: {}", e.getMessage());
                }

                return chain.proceed(request);
            });

        this.client = httpClientBuilder.build();
    }

    /**
     * Extracts headers from a request into a string format.
     *
     * @param headers The headers to extract.
     * @return A string representation of the headers.
     */
    static String getHeaders(Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false)
                .map(header -> {
                    String headerKey = header.component1();
                    String headerValue = header.component2();
                    return String.format("[%s: %s]", headerKey, headerValue);
                }).collect(Collectors.joining(", "));
    }

    /**
     * Starts the transport by initiating the SSE channel.
     *
     * @param pendingRequest A map of pending requests.
     */
    @Override
    public void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
        if (closed)
            throw new IllegalStateException("HTTP MCP transport is closed");

        setPendingRequests(pendingRequest);
        mcpSseEventListener = startSseChannel(logResponses);
    }

    /**
     * Initializes the connection with the server.
     *
     * @param request The initialization request.
     * @return A CompletableFuture that completes with the response.
     */
    @Override
    public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
        try {
            Request initializationRequest = createRequest(request);
            return completeInitialization(execute(initializationRequest, numericId(request.getId())),
                    () -> {
                        try {
                            // Build after negotiation so the very first notification carries the selected version.
                            return execute(createRequest(new InitializationNotification()), null);
                        } catch (JsonProcessingException e) {
                            return McpUtils.failedFuture(e);
                        }
                    });
        } catch (JsonProcessingException e) {
            return McpUtils.failedFuture(e);
        }
    }

    /**
     * Sends a request to the server and waits for a response.
     *
     * @param request The request to send.
     * @return A CompletableFuture that completes with the response.
     */
    @Override
    public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
        requireInitialized();

        try {
            Request req = createRequest(request);
            return execute(req, numericId(request.getId()));
        } catch (JsonProcessingException e) {
            return McpUtils.failedFuture(e);
        }
    }

    /**
     * Sends a request to the server without waiting for a response.
     *
     * @param request The request to send.
     */
    @Override
    public void sendRequestWithoutResponse(McpRequest request) {
        try {
            Request httpRequest = createRequest(request);
            execute(httpRequest, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendJson(JsonNode message) {
        try {
            Request request = requestBuilder(postUrl).header("Content-Type", "application/json")
                    .post(RequestBody.create(JsonUtils.toJsonBytes(message))).build();
            execute(request, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes an HTTP request asynchronously.
     *
     * @param request The HTTP request to execute.
     * @param id      The ID of the request.
     * @return A CompletableFuture that completes with the response.
     */
    private CompletableFuture<JsonNode> execute(Request request, Long id) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();

        if (closed) {
            future.completeExceptionally(new IOException("HTTP MCP transport is closed"));
            return future;
        }

        if (id != null)
            saveRequest(id, future);

        if (closed) {
            future.completeExceptionally(new IOException("HTTP MCP transport is closed"));
            return future;
        }

        log.info("pending request to {}", request.url());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response ignored = response) {
                    int statusCode = response.code();

                    if (!isExpectedStatusCode(statusCode))
                        future.completeExceptionally(new RuntimeException("HTTP return ERROR! Unexpected status code: " + statusCode));

                    // For messages with null ID, we don't wait for a response in the SSE channel
                    if (id == null)
                        future.complete(null);
                }
            }
        });

        return future;
    }

    /**
     * Checks if the HTTP status code indicates success.
     *
     * @param statusCode The HTTP status code.
     * @return True if the status code is in the 200-299 range, false otherwise.
     */
    private boolean isExpectedStatusCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Starts the SSE channel and waits for the initialization to complete.
     *
     * @param logResponses Flag indicating whether to log responses.
     * @return The EventSource object representing the SSE channel.
     */
    private EventSource startSseChannel(boolean logResponses) {
        Request request = requestBuilder(sseUrl).header("Accept", "text/event-stream").build();
        CompletableFuture<String> initializationFinished = new CompletableFuture<>();
        endpointReady = initializationFinished;
        SseEventListener listener = new SseEventListener(this, logResponses, initializationFinished);
        EventSource eventSource = createEventSource(request, listener);
        mcpSseEventListener = eventSource;
        if (closed) {
            eventSource.cancel();
            throw new IllegalStateException("HTTP MCP transport is closed");
        }
        int timeout = client.callTimeoutMillis() > 0 ? client.callTimeoutMillis() : Integer.MAX_VALUE;

        // wait for the SSE channel to be created, receive the POST url from the server, throw an exception if that failed
        try {
            String relativePostUrl = initializationFinished.get(timeout, TimeUnit.MILLISECONDS);
            HttpUrl base = HttpUrl.get(sseUrl);
            HttpUrl resolved = base.resolve(relativePostUrl);
            // Never forward configured credentials to a different endpoint origin.
            if (resolved == null || !base.scheme().equals(resolved.scheme())
                    || !base.host().equals(resolved.host()) || base.port() != resolved.port())
                throw new IllegalArgumentException("SSE endpoint must have the same origin as the server URL");
            postUrl = resolved.toString();
            log.debug("Received the server's POST URL: {}", postUrl);
        } catch (Exception e) {
            eventSource.cancel();
            if (e instanceof InterruptedException)
                Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        return eventSource;
    }

    /**
     * Executes the create event source operation.
     *
     * @param request  the request value.
     * @param listener the listener value.
     * @return the result of the create event source operation.
     */
    EventSource createEventSource(Request request, SseEventListener listener) {
        return EventSources.createFactory(client).newEventSource(request, listener);
    }

    /**
     * Creates an HTTP request from a BaseJsonRpcMessage.
     *
     * @param message The message to create the request from.
     * @return The HTTP request.
     * @throws JsonProcessingException If there's an error processing the JSON.
     */
    private Request createRequest(BaseJsonRpcMessage message) throws JsonProcessingException {
        return requestBuilder(postUrl).header("Content-Type", "application/json")
                .post(RequestBody.create(JsonUtils.toJsonBytes(message))).build();
    }

    private Request.Builder requestBuilder(String url) {
        Request.Builder builder = new Request.Builder().url(url);
        requestHeaders.forEach(builder::header);
        if (getNegotiatedProtocolVersion() != null)
            builder.header(StreamableHttpTransport.PROTOCOL_VERSION_HEADER, getNegotiatedProtocolVersion());
        return builder;
    }

    /**
     * Placeholder for health checks. Currently not implemented.
     */
    @Override
    public void checkHealth() {
        // no transport-specific checks right now
    }

    /**
     * Closes the transport, canceling the SSE channel and shutting down the HTTP client.
     */
    @Override
    public void close() {
        if (closed)
            return;

        closed = true;
        if (endpointReady != null)
            endpointReady.completeExceptionally(new IOException("HTTP MCP transport is closed"));
        failPendingRequests(new IOException("HTTP MCP transport is closed"));

        if (mcpSseEventListener != null)
            mcpSseEventListener.cancel();

        if (client != null) {
            client.dispatcher().cancelAll();
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}
