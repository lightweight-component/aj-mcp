package com.ajaxjs.mcp.protocol.resource;

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
