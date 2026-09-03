package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.common.Content;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * Calling Tools Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallToolResult extends McpResponse {
    private CallToolResultDetail result;
}
