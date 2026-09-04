package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Calling Tools Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallToolResult extends McpResponse {
    /**
     * Holds the result value.
     */
    private CallToolResultDetail result;
}
