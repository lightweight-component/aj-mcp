package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * The 'ResourceTemplate' object from the MCP protocol schema.
 */
@Data
public class ResourceTemplate {
    /**
     * Holds the uri template value.
     */
    String uriTemplate;

    /**
     * Holds the name value.
     */
    String name;

    /**
     * Holds the title value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String title;

    /**
     * Holds the description value.
     */
    String description;

    /**
     * Holds the mime type value.
     */
    String mimeType;
}
