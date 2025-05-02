package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.PROMPTS_GET;
import static com.ajaxjs.mcp.protocol.McpConstant.Methods.TOOLS_CALL;

/**
 * Calling Tools
 * To invoke a tool, clients send a tools/call request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallToolRequest extends McpRequest {
    String method = TOOLS_CALL;

    Params params;

    @Data
    public static class Params {
        private String name;

        private Map<String, Object> arguments;
    }
}
