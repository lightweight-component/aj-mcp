package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Image content allows including visual information in messages.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptContentImage extends PromptContent {
    /**
     * base64-encoded-image-data
     */
    String data;

    /**
     * MimeType, like "image/png"
     */
    String mimeType;

    public PromptContentImage() {
        this.type = McpConstant.PromptContentType.IMAGE;
    }
}
