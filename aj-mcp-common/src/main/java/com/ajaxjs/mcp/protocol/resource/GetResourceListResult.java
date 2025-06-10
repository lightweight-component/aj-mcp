package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Listing Resources
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
    @RequiredArgsConstructor
    @NoArgsConstructor
    public static class ResourceResult {
        @NonNull
        List<ResourceItem> resources;

        /**
         * Pagination for response.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextCursor;
    }
}
