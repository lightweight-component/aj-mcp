package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.RESOURCES_UNSUBSCRIBE_REQUEST;

/**
 * Removes a previously established resource subscription.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnsubscribeRequest extends McpRequest {
    private String method = RESOURCES_UNSUBSCRIBE_REQUEST;
    private GetResourceRequest.Params params;
}
