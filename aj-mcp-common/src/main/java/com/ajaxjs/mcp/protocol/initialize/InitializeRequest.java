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
    /**
     * Holds the method value.
     */
    private String method = McpConstant.Methods.INITIALIZE;

    /**
     * Holds the params value.
     */
    private InitializeRequestParams params;
}
