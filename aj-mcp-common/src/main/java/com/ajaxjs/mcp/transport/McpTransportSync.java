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
}
