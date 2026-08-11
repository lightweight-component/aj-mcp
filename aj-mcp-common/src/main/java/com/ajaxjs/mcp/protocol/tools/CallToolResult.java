package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.common.Content;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

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

        /** Structured result introduced in protocol revision 2025-06-18. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, Object> structuredContent;
    }
}
