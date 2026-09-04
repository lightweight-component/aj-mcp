package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Resource Detail Response
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetResourceResult extends McpResponse {
    /**
     * Holds the result value.
     */
    private GetResourceResultDetail result;
}
