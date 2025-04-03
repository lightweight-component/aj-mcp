package com.ajaxjs.mcp.client.transport.http;

import com.ajaxjs.mcp.McpUtils;
import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.InitializationNotification;
import com.ajaxjs.mcp.client.protocol.InitializeRequest;
import com.ajaxjs.mcp.client.transport.McpOperationHandler;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class HttpMcpTransport implements McpTransport {
    private static final Logger log = LoggerFactory.getLogger(HttpMcpTransport.class);
    private final String sseUrl;
    private final OkHttpClient client;
    private final boolean logResponses;
    private final boolean logRequests;
    private EventSource mcpSseEventListener;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // this is obtained from the server after initializing the SSE channel
    private volatile String postUrl;
    private volatile McpOperationHandler messageHandler;

    public HttpMcpTransport(Builder builder) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        Duration timeout = McpUtils.getOrDefault(builder.timeout, Duration.ofSeconds(60));
        httpClientBuilder.callTimeout(timeout);
        httpClientBuilder.connectTimeout(timeout);
        httpClientBuilder.readTimeout(timeout);
        httpClientBuilder.writeTimeout(timeout);
        this.logRequests = builder.logRequests;

        if (builder.logRequests)
            httpClientBuilder.addInterceptor(new RequestLoggingInterceptor());

        this.logResponses = builder.logResponses;
        sseUrl = Objects.requireNonNull(builder.sseUrl, "Missing SSE endpoint URL");
        client = httpClientBuilder.build();
    }

    @Override
    public void start(McpOperationHandler messageHandler) {
        this.messageHandler = messageHandler;
        mcpSseEventListener = startSseChannel(logResponses);
    }

    @Override
    public CompletableFuture<JsonNode> initialize(InitializeRequest operation) {
        Request httpRequest = null;
        Request initializationNotification = null;
        try {
            httpRequest = createRequest(operation);
            initializationNotification = createRequest(new InitializationNotification());
        } catch (JsonProcessingException e) {
            return McpUtils.failedFuture(e);
        }

        final Request finalInitializationNotification = initializationNotification;
        return execute(httpRequest, operation.getId()).thenCompose(originalResponse -> execute(finalInitializationNotification, null).thenCompose(nullNode -> CompletableFuture.completedFuture(originalResponse)));
    }

    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(ClientMessage operation) {
        try {
            Request httpRequest = createRequest(operation);
            return execute(httpRequest, operation.getId());
        } catch (JsonProcessingException e) {
            return McpUtils.failedFuture(e);
        }
    }

    @Override
    public void executeOperationWithoutResponse(ClientMessage operation) {
        try {
            Request httpRequest = createRequest(operation);
            execute(httpRequest, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<JsonNode> execute(Request request, Long id) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        if (id != null)
            messageHandler.startOperation(id, future);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
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

    private boolean isExpectedStatusCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private EventSource startSseChannel(boolean logResponses) {
        Request request = new Request.Builder().url(sseUrl).build();
        CompletableFuture<String> initializationFinished = new CompletableFuture<>();
        SseEventListener listener = new SseEventListener(messageHandler, logResponses, initializationFinished);
        EventSource eventSource = EventSources.createFactory(client).newEventSource(request, listener);

        // wait for the SSE channel to be created, receive the POST url from the server, throw an exception if that failed
        try {
            int timeout = client.callTimeoutMillis() > 0 ? client.callTimeoutMillis() : Integer.MAX_VALUE;
            String relativePostUrl = initializationFinished.get(timeout, TimeUnit.MILLISECONDS);
            postUrl = buildAbsolutePostUrl(relativePostUrl);
            log.debug("Received the server's POST URL: {}", postUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return eventSource;
    }

    private String buildAbsolutePostUrl(String relativePostUrl) {
        try {
            return URI.create(this.sseUrl).resolve(relativePostUrl).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Request createRequest(ClientMessage message) throws JsonProcessingException {
        return new Request.Builder()
                .url(postUrl)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(OBJECT_MAPPER.writeValueAsBytes(message)))
                .build();
    }

    @Override
    public void close() throws IOException {
        if (mcpSseEventListener != null)
            mcpSseEventListener.cancel();

        if (client != null)
            client.dispatcher().executorService().shutdown();

    }

    public static class Builder {

        private String sseUrl;
        private Duration timeout;
        private boolean logRequests = false;
        private boolean logResponses = false;

        /**
         * The initial URL where to connect to the server and request a SSE
         * channel.
         */
        public Builder sseUrl(String sseUrl) {
            this.sseUrl = sseUrl;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public HttpMcpTransport build() {
            return new HttpMcpTransport(this);
        }
    }
}
