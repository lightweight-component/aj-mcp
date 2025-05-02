package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * The 'Resource' object from the MCP protocol schema.
 */
@Data
public class ResourceItem {
    /**
     * Unique identifier for the resource
     */
    String uri;

    /**
     * Human-readable name
     */
    String name;

    /**
     * Optional MIME type
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String mimeType;

    /**
     * Optional description
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String description;

    /**
     * Optional size in bytes
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long size;
}
