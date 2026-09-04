package com.ajaxjs.mcp.protocol.utils.ping;

import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents ping request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PingRequest extends McpRequest {
    /**
     * Holds the method value.
     */
    private final String method = McpConstant.Methods.PING;
}
