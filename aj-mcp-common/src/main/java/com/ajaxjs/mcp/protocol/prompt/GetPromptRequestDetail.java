package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpRequestRaw;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.PROMPTS_GET;

/**
 * To retrieve a specific prompt, clients send a prompts/get request.
 * Arguments may be auto-completed through the completion API.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetPromptRequestDetail extends McpRequestRaw {
    String method = PROMPTS_GET;

    Params params;

    @Data
    public static class Params {
        String name;

        Map<String, Object> arguments;
    }
}
