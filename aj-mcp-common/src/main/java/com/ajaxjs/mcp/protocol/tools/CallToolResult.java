package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.common.Content;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Calling Tools Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallToolResult extends McpResponse {
    private CallToolResultDetail result;

    @Data
    public static class CallToolResultDetail {
        Boolean isError = false;

        List<Content> content;
    }
}
