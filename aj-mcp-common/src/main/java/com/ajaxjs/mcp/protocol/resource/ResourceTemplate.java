package com.ajaxjs.mcp.protocol.resource;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The 'ResourceTemplate' object from the MCP protocol schema.
 */
@Data
public class ResourceTemplate {
    String uriTemplate;

    String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String title;

    String description;

    String mimeType;
}
