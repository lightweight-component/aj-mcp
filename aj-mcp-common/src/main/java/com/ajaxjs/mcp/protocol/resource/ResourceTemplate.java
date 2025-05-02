package com.ajaxjs.mcp.protocol.resource;

import lombok.Data;

/**
 * The 'ResourceTemplate' object from the MCP protocol schema.
 */
@Data
public class ResourceTemplate {
    String uriTemplate;

    String name;

    String description;

    String mimeType;
}
