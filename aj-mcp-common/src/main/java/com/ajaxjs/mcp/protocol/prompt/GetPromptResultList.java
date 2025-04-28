package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Listing Prompts
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetPromptResultList extends McpResponse {
    private PromptResult result;

    @Data
    public static class PromptResult {
        List<PromptItem> prompts;
    }
}
