package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents resource templates result detail.
 */
@Data
@NoArgsConstructor
public class ResourceTemplatesResultDetail {
    /**
     * Holds the resource templates value.
     */
    List<ResourceTemplate> resourceTemplates;

    /**
     * Holds the next cursor value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String nextCursor;
}
