package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Listing Tools
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetToolListResult extends McpResponse {
    private GetToolListResultToolList result;
}
