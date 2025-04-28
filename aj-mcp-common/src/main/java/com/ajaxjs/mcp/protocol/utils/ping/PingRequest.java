package com.ajaxjs.mcp.protocol.utils.ping;

import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PingRequest extends McpRequest {
    private final String method = McpConstant.Methods.PING;
}
