package com.ajaxjs.mcp.protocol.utils.completion;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Represents completion result.
 */
@Data
@AllArgsConstructor
public class CompletionResult {
    /**
     * Array of suggestions (max 100).
     * Servers return an array of completion values ranked by relevance, with Maximum 100 items per response
     */
    private List<String> values;

    /**
     * Optional total number of available matches
     */
    private Integer total;

    /**
     * Boolean indicating if additional results exist
     */
    private Boolean hasMore;
}