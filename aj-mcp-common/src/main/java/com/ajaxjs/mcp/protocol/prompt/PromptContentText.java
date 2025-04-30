package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Text content represents plain text messages.
 * This is the most common content type used for natural language interactions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptContentText extends PromptContent {
    String text;

    public PromptContentText() {
        this.type = McpConstant.PromptContentType.TEXT;
    }

    public PromptContentText(String text) {
        this();
        this.text = text;
    }
}
