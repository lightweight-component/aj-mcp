package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.RESOURCES_TEMPLATES_LIST;

/**
 * Resource Template allows servers to expose parameterized resources using URI templates. Arguments may be auto-completed through the completion API.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetResourceTemplateListRequest extends McpRequest {
    String method = RESOURCES_TEMPLATES_LIST;
}
