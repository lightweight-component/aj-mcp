package com.ajaxjs.mcp.transport;

import java.io.Closeable;

/**
 * 同步的 MCP 通讯
 */
public interface McpTransportSync extends Closeable {
    void start();

    /**
     * Handle the message from client, it's JSON string, then do the business, finally return the result to client as Json string.
     *
     * @param rawJson The message from client, it's JSON string.
     * @return The result to client as Json string
     */
    String handle(String rawJson);

    void initialize();

    /**
     * Sends a server-originated message to one logical transport session.
     */
    default void send(String sessionId, String json) {
        throw new UnsupportedOperationException("Server-originated messages are not supported by this transport");
    }

    default void broadcast(String json) {
        throw new UnsupportedOperationException("Broadcast is not supported by this transport");
    }
}
