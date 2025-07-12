package com.ajaxjs.mcp.protocol.utils.completion;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Complete Detail Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompleteResult extends McpResponse {
    private CompleteResultDetail result;

    @Data
    @AllArgsConstructor
    public static class CompleteResultDetail {
        CompletionResult completion;
    }

    @Data
    @AllArgsConstructor
    public static class CompletionResult {
        /**
         * Array of suggestions (max 100).
         * Servers return an array of completion values ranked by relevance, with Maximum 100 items per response
         */
        List<String> values;

        /**
         * Optional total number of available matches
         */
        Integer total;

        /**
         * Boolean indicating if additional results exist
         */
        Boolean hasMore;
    }
}
