package com.ajaxjs.mcp.protocol.initialize;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Initialize from client request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InitializeRequest extends BaseJsonRpcMessage {
    private String method = McpConstant.Methods.INITIALIZE;

    private InitializeRequestParams params;
}
