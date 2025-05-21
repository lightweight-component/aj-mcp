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
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implements transport for MCP (Microservice Communication Protocol) using HTTP and SSE (Server-Sent Events).
 * This class is responsible for managing the connection with the server, sending requests, and handling responses.
 */
@Slf4j
public class HttpMcpTransport extends McpTransport {
    /**
     * The URL for the SSE (Server-Sent Events) connection.
     */
    private String sseUrl;

    /**
     * The HTTP client used for making requests.
     */
    private OkHttpClient client;

    /**
     * Flag indicating whether to log server responses.
     */
    private boolean logResponses;

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
     * Constructor for creating an instance with only the SSE URL.
     *
     * @param sseUrl The SSE URL.
     */
    public HttpMcpTransport(String sseUrl) {
        this.sseUrl = sseUrl;
    }

    @Builder
    public HttpMcpTransport(String sseUrl, boolean logResponses, boolean logRequests) {
        Objects.requireNonNull(sseUrl, "Missing SSE endpoint URL");
        this.sseUrl = sseUrl;
        this.logRequests = logRequests;
        this.logResponses = logResponses;

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        Duration timeout = Duration.ofSeconds(60);
        httpClientBuilder.callTimeout(timeout);
        httpClientBuilder.connectTimeout(timeout);
        httpClientBuilder.readTimeout(timeout);
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
        Request httpRequest;
        Request initializationNotification;

        try {
            httpRequest = createRequest(request);
            initializationNotification = createRequest(new InitializationNotification());
        } catch (JsonProcessingException e) {
            return McpUtils.failedFuture(e);
        }

        return execute(httpRequest, request.getId()).thenCompose(originalResponse -> execute(initializationNotification, null)
                .thenCompose(nullNode -> CompletableFuture.completedFuture(originalResponse)));
    }

    /**
     * Sends a request to the server and waits for a response.
     *
     * @param request The request to send.
     * @return A CompletableFuture that completes with the response.
     */
    @Override
    public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
        try {
            Request req= createRequest(request);
            return execute(req, request.getId());
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

    /**
     * Executes an HTTP request asynchronously.
     *
     * @param request The HTTP request to execute.
     * @param id      The ID of the request.
     * @return A CompletableFuture that completes with the response.
     */
    private CompletableFuture<JsonNode> execute(Request request, Long id) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        if (id != null)
            saveRequest(id, future);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                int statusCode = response.code();

                if (!isExpectedStatusCode(statusCode))
                    future.completeExceptionally(new RuntimeException("Unexpected status code: " + statusCode));

                // For messages with null ID, we don't wait for a response in the SSE channel
                if (id == null)
                    future.complete(null);
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
        Request request = new Request.Builder().url(sseUrl).build();
        CompletableFuture<String> initializationFinished = new CompletableFuture<>();
        SseEventListener listener = new SseEventListener(this, logResponses, initializationFinished);
        EventSource eventSource = EventSources.createFactory(client).newEventSource(request, listener);
        int timeout = client.callTimeoutMillis() > 0 ? client.callTimeoutMillis() : Integer.MAX_VALUE;

        // wait for the SSE channel to be created, receive the POST url from the server, throw an exception if that failed
        try {
            String relativePostUrl = initializationFinished.get(timeout, TimeUnit.MILLISECONDS);
            postUrl = URI.create(sseUrl).resolve(relativePostUrl).toString();
            log.debug("Received the server's POST URL: {}", postUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return eventSource;
    }

    /**
     * Creates an HTTP request from a BaseJsonRpcMessage.
     *
     * @param message The message to create the request from.
     * @return The HTTP request.
     * @throws JsonProcessingException If there's an error processing the JSON.
     */
    private Request createRequest(BaseJsonRpcMessage message) throws JsonProcessingException {
        return new Request.Builder().url(postUrl).header("Content-Type", "application/json")
                .post(RequestBody.create(JsonUtils.OBJECT_MAPPER.writeValueAsBytes(message))).build();
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
        if (mcpSseEventListener != null)
            mcpSseEventListener.cancel();

        if (client != null)
            client.dispatcher().executorService().shutdown();
    }
}
