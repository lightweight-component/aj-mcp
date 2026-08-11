package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Listing Resource Templates
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResourceTemplateResult extends McpResponse {
    private ResourceTemplatesResult result;

    public ResourceTemplateResult(ResourceTemplatesResult result) {
        this.result = result;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceTemplatesResult {
        List<ResourceTemplate> resourceTemplates;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextCursor;
    }
}
