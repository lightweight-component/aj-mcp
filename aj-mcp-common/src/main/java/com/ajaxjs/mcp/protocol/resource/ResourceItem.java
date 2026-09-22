package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * The 'Resource' object from the MCP protocol schema.
 */
@Data
public class ResourceItem extends com.ajaxjs.mcp.protocol.common.Metadata {
    /**
     * Optional hints; omitted when unset to preserve existing wire output.
     */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private com.ajaxjs.mcp.protocol.common.Annotations annotations;

    /**
     * Unique identifier for the resource
     */
    String uri;

    /**
     * Human-readable name
     */
    String name;

    /**
     * Holds the title value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String title;

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
