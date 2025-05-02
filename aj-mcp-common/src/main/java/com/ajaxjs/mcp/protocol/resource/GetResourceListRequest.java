package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.RESOURCES_LIST;

/**
 * Listing Resources
 * To discover available resources, clients send a resources/list request. This operation supports pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetResourceListRequest extends McpRequest {
    String method = RESOURCES_LIST;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    Cursor params;
}
