package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Listing Resources
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetResourceListResult extends McpResponse {
    /**
     * Holds the result value.
     */
    private GetResourceListResultDetail result;

    /**
     * Creates a new get resource list result.
     *
     * @param result the result value.
     */
    public GetResourceListResult(GetResourceListResultDetail result) {
        this.result = result;
    }
}
