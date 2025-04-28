package com.ajaxjs.mcp.client.protocol.resource;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The 'TextResourceContents' object from the MCP protocol schema.
 */
@Data
@AllArgsConstructor
public class TextResourceContents implements ResourceContents {
    String uri;

    String text;

    String mimeType;

    @Override
    public Type getType() {
        return Type.TEXT;
    }
}
