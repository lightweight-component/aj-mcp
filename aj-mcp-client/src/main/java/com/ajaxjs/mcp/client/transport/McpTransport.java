package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface McpTransport extends Closeable {
    /**
     * Creates a connection to the MCP server (runs the server as a subprocess if needed).
     * This does NOT yet send the "initialize" message to negotiate capabilities.
     */
    void start(Map<Long, CompletableFuture<JsonNode>> pendingOperations);

    /**
     * Handles incoming JSON messages from the MCP server.
     * This method processes different types of messages based on their content.
     * It checks for the presence of an "id" field to determine if it's a response to a pending operation, and handles "ping" method messages specifically.
     * Additionally, it processes log messages under the "notifications/message" method.
     *
     * @param message The JSON message to be handled, represented as a JsonNode object.
     */
    void handle(JsonNode message);

    /**
     * Sends the "initialize" message to the MCP server to negotiate
     * capabilities, supported protocol version etc. When this method
     * returns successfully, the transport is fully initialized and ready to
     * be used. This has to be called AFTER the "start" method.
     */
    CompletableFuture<JsonNode> initialize(InitializeRequest request);

    /**
     * Executes an operation that expects a response from the server.
     *
     * @param request The request to be sent.
     * @return The future response from the server.
     */
    CompletableFuture<JsonNode> executeOperationWithResponse(McpRequest request);

    /**
     * Sends a message that does not expect a response from the server. The 'id' field of the message should be null.
     *
     * @param request The request to be sent.
     */
    void executeOperationWithoutResponse(McpRequest request);

    /**
     * Performs transport-specific health checks, if applicable. This is called
     * by `McpClient.checkHealth()` as the first check before performing a check
     * by sending a 'ping' over the MCP protocol. The purpose is that the
     * transport may have some specific and faster ways to detect that it is broken,
     * like for example, the STDIO transport can fail the check if it detects
     * that the server subprocess isn't alive anymore.
     */
    void checkHealth();
}
