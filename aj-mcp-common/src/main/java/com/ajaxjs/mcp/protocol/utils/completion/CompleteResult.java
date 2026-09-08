package com.ajaxjs.mcp.protocol.utils.completion;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Complete Detail Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompleteResult extends McpResponse {
    /**
     * Holds the result value.
     */
    private CompleteResultDetail result;
}
