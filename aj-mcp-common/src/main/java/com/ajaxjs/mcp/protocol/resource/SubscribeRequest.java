package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.RESOURCES_SUBSCRIBE_REQUEST;

/**
 * Subscriptions
 * The protocol supports optional subscriptions to resource changes. Clients can subscribe to specific resources and receive notifications when they change.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscribeRequest extends McpRequest {
    String method = RESOURCES_SUBSCRIBE_REQUEST;

    GetResourceRequest.Params params;
}
