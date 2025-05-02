package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * List Changed Notification from the server
 * When the list of available prompts changes, servers that declared the listChanged capability SHOULD send a notification.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptListChangedNotification extends BaseJsonRpcMessage {
    String method = McpConstant.Methods.PROMPTS_LIST_CHANGED_NOTIFICATION;
}
