package com.ajaxjs.mcp.protocol.initialize;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Initialize from server.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InitializeResponse extends McpResponse {
    private InitializeResponseResult result;
}
