package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Listing Tools
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetToolListResult extends McpResponse {
    private ToolList result;

    public GetToolListResult(ToolList result) {
        this.result = result;
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    @NoArgsConstructor
    public static class ToolList {
        @NonNull
        private List<ToolItem> tools;

        /**
         * Pagination for response.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String nextCursor;
    }
}
