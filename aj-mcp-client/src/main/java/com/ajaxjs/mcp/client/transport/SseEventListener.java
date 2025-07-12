package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.concurrent.CompletableFuture;

/**
 * Custom EventSourceListener for handling Server-Sent Events (SSE).
 * Used to process events received from the server, including messages, endpoint events, and connection status.
 */
@Slf4j
@AllArgsConstructor
public class SseEventListener extends EventSourceListener {
    /**
     * The transport object for processing message events
     */
    private final McpTransport transport;

    /**
     * Flag indicating whether to log event messages
     */
    private final boolean logEvents;

    /**
     * A CompletableFuture for handling the endpoint event, indicating the completion of initialization.
     * This will contain the POST url for sending commands to the server.
     */
    private final CompletableFuture<String> initializationFinished;

    /**
     * Process messages from the event source.
     *
     * @param eventSource The source of the event
     * @param id          The unique identifier of the event
     * @param type        The type of the event, determines how the event is processed
     * @param data        The data carried by the event, specific content depends on the event type
     */
    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        log.info("Event: {}, data: {}", type, data);
        if ("message".equals(type)) {
            if (logEvents)
                log.info("> {}", data);

            JsonNode jsonNode = JsonUtils.json2Node(data);
            transport.handle(jsonNode);
        } else if ("endpoint".equals(type)) {
            if (initializationFinished.isDone()) {
                log.warn("Received endpoint event after initialization");
                return;
            }

            initializationFinished.complete(data);
        }
    }

    /**
     * Handles failure events from the EventSource.
     * This method is called when an error occurs in the EventSource, to handle errors under different circumstances.
     *
     * @param eventSource The event source where the error occurred
     * @param t           The cause of the error, if available
     * @param response    The server response, if available
     */
    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (!initializationFinished.isDone()) {
            if (t != null)
                initializationFinished.completeExceptionally(t);
            else if (response != null)
                initializationFinished.completeExceptionally(new RuntimeException("The server returned: " + response.message()));
        }

        if (t != null && (t.getMessage() == null || !t.getMessage().toLowerCase().contains("Socket closed".toLowerCase())))
            log.warn("SSE channel failure", t);
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        log.debug("Connected to SSE channel at {}", response.request().url());
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log.debug("SSE channel closed");
    }
}
