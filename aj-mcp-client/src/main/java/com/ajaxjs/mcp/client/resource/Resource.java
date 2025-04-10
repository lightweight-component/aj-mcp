package com.ajaxjs.mcp.client.resource;

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
