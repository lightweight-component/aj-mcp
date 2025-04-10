package com.ajaxjs.mcp.client.resource;

/**
 * A holder for either a 'McpTextResourceContents' or 'McpBlobResourceContents'
 * object from the MCP protocol schema.
 */
public interface ResourceContents {
    Type getType();

    enum Type {
        TEXT,
        BLOB
    }
}
