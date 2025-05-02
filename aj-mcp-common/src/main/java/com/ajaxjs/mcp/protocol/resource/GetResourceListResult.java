package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Listing Prompts
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetResourceListResult extends McpResponse {
    private ResourceResult result;

    public GetResourceListResult(ResourceResult result) {
        this.result = result;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceResult {
        List<ResourceItem> resources;
    }
}
