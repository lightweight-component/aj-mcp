package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.PROMPTS_GET;

/**
 * To retrieve a specific prompt, clients send a prompts/get request.
 * Arguments may be auto-completed through the completion API.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetPromptRequest extends McpRequest {
    /**
     * Holds the method value.
     */
    String method = PROMPTS_GET;

    /**
     * Holds the params value.
     */
    GetPromptRequestParams params;
}
