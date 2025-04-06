package com.ajaxjs.mcp.client.logging;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A handler that decides what to do with received log messages from an MCP server.
 */
public interface McpLogMessageHandler {
    void handleLogMessage(McpLogMessage message);

    /**
     * Parses a McpLogMessage from the contents of the 'params' object inside a 'notifications/message' message.
     */
    static McpLogMessage fromJson(JsonNode json) {
        McpLogLevel level = from(json.get("level").asText());
        JsonNode loggerNode = json.get("logger");
        String logger = loggerNode != null ? loggerNode.asText() : null;
        JsonNode data = json.get("data");

        return new McpLogMessage(level, logger, data);
    }

    static McpLogLevel from(String val) {
        try {
            return McpLogLevel.valueOf(val.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
