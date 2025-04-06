package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.logging.McpLogMessage;
import com.ajaxjs.mcp.client.logging.McpLogMessageHandler;
import com.ajaxjs.mcp.client.protocol.PingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles incoming messages from the MCP server. Transport implementations
 * should call the "handle" method on each received message. A transport also has
 * to call "startOperation" when before starting an operation that requires a response
 * to register its ID in the map of pending operations.
 */
@Slf4j
@AllArgsConstructor
public class McpOperationHandler {
    private final Map<Long, CompletableFuture<JsonNode>> pendingOperations;

    private final McpTransport transport;

    private final Consumer<McpLogMessage> logMessageConsumer;

    /**
     * Handles incoming JSON messages.
     * This method processes different types of messages based on their content.
     * It checks for the presence of an "id" field to determine if it's a response to a pending operation, and handles "ping" method messages specifically.
     * Additionally, it processes log messages under the "notifications/message" method.
     *
     * @param message The JSON message to be handled, represented as a JsonNode object.
     */
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
                        transport.executeOperationWithoutResponse(new PingResponse(messageId));
                        return;
                    }
                }

                log.warn("Received response for unknown message id: {}", messageId);
            }
        } else if (message.has("method") && message.get("method").asText().equals("notifications/message")) {
            // this is a log message
            if (message.has("params")) {
                if (logMessageConsumer != null)
                    logMessageConsumer.accept(McpLogMessageHandler.fromJson(message.get("params")));
            } else
                log.warn("Received log message without params: {}", message);
        } else
            log.warn("Received unknown message: {}", message);
    }

    public void startOperation(Long id, CompletableFuture<JsonNode> future) {
        pendingOperations.put(id, future);
    }
}
