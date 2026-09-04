package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Listing Resource Templates
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class ResourceTemplateResult extends McpResponse {
    /**
     * Holds the result value.
     */
    private final ResourceTemplatesResultDetail result;
}
