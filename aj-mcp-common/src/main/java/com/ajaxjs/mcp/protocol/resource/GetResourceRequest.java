package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.RESOURCES_READ;

/**
 * Reading Resources
 * To retrieve resource contents, clients send a resources/read request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetResourceRequest extends McpRequest {
    String method = RESOURCES_READ;

    Params params;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Params {
        String uri;
    }
}
