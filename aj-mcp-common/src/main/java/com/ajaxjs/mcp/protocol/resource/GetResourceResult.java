package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Resource Detail Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetResourceResult extends McpResponse {
    private ResourceResultDetail result;

    @Data
    public static class ResourceResultDetail {
        List<ResourceContent> contents;
    }
}
