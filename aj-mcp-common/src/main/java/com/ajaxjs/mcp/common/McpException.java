package com.ajaxjs.mcp.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/**
 * A business exception raised over the MCP protocol
 */
@Getter
public class McpException extends RuntimeException {
    /**
     * Holds the error code value.
     */
    private final int errorCode;
    /**
     * Holds the error message value.
     */
    private final String errorMessage;

    /**
     * Creates a new mcp exception.
     *
     * @param errorCode    the error code value.
     * @param errorMessage the error message value.
     */
    public McpException(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getMessage() {
        return "Code: " + errorCode + ", message: " + errorMessage;
    }

    /**
     * Executes the check for errors operation.
     *
     * @param mcpMessage the mcp message value.
     */
    public static void checkForErrors(JsonNode mcpMessage) {
        if (mcpMessage.has("error")) {
            JsonNode errorNode = mcpMessage.get("error");
            throw new McpException(errorNode.get("code").asInt(), errorNode.get("message").asText());
        }
    }
}
