package com.ajaxjs.mcp.protocol.resource;

import com.ajaxjs.mcp.protocol.McpResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Listing Resource Templates
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResourceTemplateResult extends McpResponse {
    private ResourceTemplatesResultDetail result;

    public ResourceTemplateResult(ResourceTemplatesResultDetail result) {
        this.result = result;
    }
}
