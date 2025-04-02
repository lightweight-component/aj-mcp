package com.ajaxjs.mcp.client.logging;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class McpLogMessage {
    McpLogLevel level;

    String logger;

    JsonNode data;

    /**
     * Parses a McpLogMessage from the contents of the 'params' object inside a 'notifications/message' message.
     */
    public static McpLogMessage fromJson(JsonNode json) {
        McpLogLevel level = McpLogLevel.from(json.get("level").asText());
        JsonNode loggerNode = json.get("logger");
        String logger = loggerNode != null ? loggerNode.asText() : null;
        JsonNode data = json.get("data");

        return new McpLogMessage(level, logger, data);
    }
}
