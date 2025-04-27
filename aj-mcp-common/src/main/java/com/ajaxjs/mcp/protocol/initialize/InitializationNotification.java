package com.ajaxjs.mcp.protocol.initialize;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * After successful initialization, the client MUST send an initialized notification to indicate it is ready to begin normal operations
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InitializationNotification extends BaseJsonRpcMessage {
    private String method = McpConstant.Methods.NOTIFICATION_INITIALIZED;
}
