package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * List Changed Notification from the server
 * When the list of available tools changes, servers that declared the listChanged capability SHOULD send a notification.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolListChangedNotification extends BaseJsonRpcMessage {
    String method = McpConstant.Methods.TOOLS_LIST_CHANGED_NOTIFICATION;
}
