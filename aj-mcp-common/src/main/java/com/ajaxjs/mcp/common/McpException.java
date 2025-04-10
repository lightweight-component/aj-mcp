package com.ajaxjs.mcp.common;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A business exception raised over the MCP protocol
 */
public class McpException extends RuntimeException {
    private final int errorCode;
    private final String errorMessage;

    public McpException(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getMessage() {
        return "Code: " + errorCode + ", message: " + errorMessage;
    }

    public static void checkForErrors(JsonNode mcpMessage) {
        if (mcpMessage.has("error")) {
            JsonNode errorNode = mcpMessage.get("error");
            throw new McpException(errorNode.get("code").asInt(), errorNode.get("message").asText());
        }
    }
}
