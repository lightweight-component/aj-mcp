package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.protocol.ping.PingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Slf4j
public abstract class BaseTransport implements McpTransport {
    private Map<Long, CompletableFuture<JsonNode>> pendingOperations;

    /**
     * A transport also has to call "startOperation" when before starting an operation that requires a response
     * to register its ID in the map of pending operations.
     *
     * @param id     The request id
     * @param future The request going to send
     */
    public void startOperation(Long id, CompletableFuture<JsonNode> future) {
        pendingOperations.put(id, future);
    }

    @Override
    public void handle(JsonNode message) {
        if (message.has("id")) {
            long messageId = message.get("id").asLong();
            CompletableFuture<JsonNode> op = pendingOperations.remove(messageId);

            if (op != null)
                op.complete(message);
            else {
                if (message.has("method")) {
                    String method = message.get("method").asText();

                    if (method.equals("ping")) {
                        executeOperationWithoutResponse(new PingResponse(messageId));
                        return;
                    }
                }

                log.warn("Received response for unknown message id: {}", messageId);
            }
        } else if (message.has("method") && message.get("method").asText().equals("notifications/message")) {
            // this is a log message
            if (message.has("params")) {
                log.info(message.get("params").asText());
            } else
                log.warn("Received log message without params: {}", message);
        } else
            log.warn("Received unknown message: {}", message);
    }

    public void setPendingOperations(Map<Long, CompletableFuture<JsonNode>> pendingOperations) {
        this.pendingOperations = pendingOperations;
    }

    public Map<Long, CompletableFuture<JsonNode>> getPendingOperations() {
        return pendingOperations;
    }

}
