package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Listing Prompts
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetPromptListResult extends McpResponse {
    private PromptResult result;

    public GetPromptListResult(PromptResult result) {
        this.result = result;
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    @NoArgsConstructor
    public static class PromptResult {
        @NonNull
        private List<PromptItem> prompts;

        /**
         * Pagination for response.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String nextCursor;
    }
}
