package com.ajaxjs.mcp.client.transport.http;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class SseEventListener extends EventSourceListener {
    private final boolean logEvents;
    // this will contain the POST url for sending commands to the server
    private final CompletableFuture<String> initializationFinished;
    private final McpTransport transport;

    public SseEventListener(McpTransport transport, boolean logEvents, CompletableFuture initializationFinished) {
        this.transport = transport;
        this.logEvents = logEvents;
        this.initializationFinished = initializationFinished;
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log.debug("SSE channel closed");
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        if (type.equals("message")) {
            if (logEvents)
                log.info("< {}", data);

            JsonNode jsonNode = JsonUtils.json2Node(data);
            transport.handle(jsonNode);
        } else if (type.equals("endpoint")) {
            if (initializationFinished.isDone()) {
                log.warn("Received endpoint event after initialization");
                return;
            }

            initializationFinished.complete(data);
        }
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (!initializationFinished.isDone()) {
            if (t != null)
                initializationFinished.completeExceptionally(t);
            else if (response != null)
                initializationFinished.completeExceptionally(new RuntimeException("The server returned: " + response.message()));
        }

        if (t != null && (t.getMessage() == null || !t.getMessage().contains("Socket closed")))
            log.warn("SSE channel failure", t);
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.debug("Connected to SSE channel at {}", response.request().url());
    }
}
