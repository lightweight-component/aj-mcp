package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Text content represents plain text messages.
 * This is the most common content type used for natural language interactions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContentText extends Content {
    String text;

    public ContentText() {
        this.type = McpConstant.ContentType.TEXT;
    }

    public ContentText(String text) {
        this();
        this.text = text;
    }
}
