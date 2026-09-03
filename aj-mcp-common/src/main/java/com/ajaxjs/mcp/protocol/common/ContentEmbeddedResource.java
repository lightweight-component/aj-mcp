package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Embedded resources allow referencing server-side resources directly in messages.
 * Embedded resources enable prompts to seamlessly incorporate server-managed content like documentation, code samples,
 * or other reference materials directly into the conversation flow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContentEmbeddedResource extends Content {
    /**
     * MimeType, like "image/png"
     */
    ContentEmbeddedResourceDetail resource;

    public ContentEmbeddedResource() {
        this.type = McpConstant.ContentType.RESOURCE;
    }
}
