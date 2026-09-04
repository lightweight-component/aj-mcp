package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.RESOURCES_READ;

/**
 * Reading Resources
 * To retrieve resource contents, clients send a resources/read request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetResourceRequest extends McpRequest {
    /**
     * Holds the method value.
     */
    String method = RESOURCES_READ;

    /**
     * Holds the params value.
     */
    GetResourceRequestParams params;
}
