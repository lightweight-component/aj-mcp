package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Image content allows including visual information in messages.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContentImage extends Content {
    /**
     * base64-encoded-image-data
     */
    String data;

    /**
     * MimeType, like "image/png"
     */
    String mimeType;

    public ContentImage() {
        this.type = McpConstant.ContentType.IMAGE;
    }
}
