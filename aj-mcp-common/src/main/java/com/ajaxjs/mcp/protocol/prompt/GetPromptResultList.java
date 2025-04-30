package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Listing Prompts
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetPromptResultList extends McpResponse {
    private PromptResult result;

    public GetPromptResultList(PromptResult result) {
        this.result = result;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PromptResult {
        List<PromptItem> prompts;
    }
}
