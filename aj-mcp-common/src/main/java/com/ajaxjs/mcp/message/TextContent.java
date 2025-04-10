package com.ajaxjs.mcp.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.ajaxjs.mcp.message.ContentType.TEXT;

/**
 * Represents a text content.
 */
@Data
@AllArgsConstructor
public class TextContent implements Content {
    private final String text;

    @Override
    public ContentType getType() {
        return TEXT;
    }
}