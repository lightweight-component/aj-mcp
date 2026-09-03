package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Listing Prompts
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetPromptListResult extends McpResponse {
    private GetPromptListResultDetail result;

    public GetPromptListResult(GetPromptListResultDetail result) {
        this.result = result;
    }
}
