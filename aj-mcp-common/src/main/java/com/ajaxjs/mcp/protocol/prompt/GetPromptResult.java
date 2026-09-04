package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Prompts Detail Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetPromptResult extends McpResponse {
    /**
     * Holds the result value.
     */
    private GetPromptResultDetail result;
}
