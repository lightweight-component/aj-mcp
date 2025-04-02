package com.ajaxjs.mcp.resource;

import lombok.Data;

/**
 * The 'Resource' object from the MCP protocol schema.
 */
@Data
public class Resource {
    String uri;

    String name;

    String description;

    String mimeType;
}
