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
}
