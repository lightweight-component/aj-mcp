package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Listing Prompts
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetPromptListResult extends McpResponse {
    /**
     * Holds the result value.
     */
    private GetPromptListResultDetail result;

    /**
     * Creates a new get prompt list result.
     *
     * @param result the result value.
     */
    public GetPromptListResult(GetPromptListResultDetail result) {
        this.result = result;
    }
}
