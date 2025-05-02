package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.RESOURCES_LIST;
import static com.ajaxjs.mcp.protocol.McpConstant.Methods.TOOLS_LIST;

/**
 * Listing Tools
 * To discover available tools, clients send a tools/list request. This operation supports pagination.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetToolListRequest extends McpRequest {
    String method = TOOLS_LIST;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    Cursor params;
}
