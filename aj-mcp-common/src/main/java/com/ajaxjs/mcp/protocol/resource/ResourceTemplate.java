package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * The 'ResourceTemplate' object from the MCP protocol schema.
 */
@Data
public class ResourceTemplate extends com.ajaxjs.mcp.protocol.common.Metadata {
    /**
     * Optional hints; omitted when unset to preserve existing wire output.
     */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private com.ajaxjs.mcp.protocol.common.Annotations annotations;

    /**
     * Holds the uri template value.
     */
    private String uriTemplate;

    /**
     * Holds the name value.
     */
    private String name;

    /**
     * Holds the title value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String title;

    /**
     * Holds the description value.
     */
    private String description;

    /**
     * Holds the mime type value.
     */
    private String mimeType;
}
