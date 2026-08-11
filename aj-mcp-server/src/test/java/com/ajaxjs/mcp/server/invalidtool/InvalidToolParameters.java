package com.ajaxjs.mcp.server.invalidtool;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;

@McpService
public class InvalidToolParameters {
    @Tool
    public String missingAnnotation(String value) {
        return value;
    }
}
