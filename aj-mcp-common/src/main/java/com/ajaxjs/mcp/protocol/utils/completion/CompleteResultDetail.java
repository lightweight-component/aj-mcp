package com.ajaxjs.mcp.protocol.utils.completion;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents complete result detail.
 */
@Data
@AllArgsConstructor
public class CompleteResultDetail {
    /**
     * Holds the completion value.
     */
    private CompletionResult completion;
}