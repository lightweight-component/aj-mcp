package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Prompts Detail Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetPromptResultDetail extends McpResponse {
    private PromptResultDetail result;

    @Data
    public static class PromptResultDetail {
        String description;
        
        List<PromptMessage> messages;
    }
}
