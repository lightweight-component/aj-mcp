package com.ajaxjs.mcp.transport;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Closeable;

/**
 * 同步的 MCP 通讯
 */
public interface McpTransportSync extends Closeable {
    void start();

    void handle(JsonNode message);

    void initialize();
}
