package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Update Notification.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionUpdateNotification extends BaseJsonRpcMessage {
    String method = McpConstant.Methods.RESOURCE_UPDATE_NOTIFICATION;

    GetResourceRequest.Params params;
}
