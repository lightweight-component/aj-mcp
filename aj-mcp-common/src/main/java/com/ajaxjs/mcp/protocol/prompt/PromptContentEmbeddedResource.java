package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Embedded resources allow referencing server-side resources directly in messages.
 * Embedded resources enable prompts to seamlessly incorporate server-managed content like documentation, code samples,
 * or other reference materials directly into the conversation flow.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptContentEmbeddedResource extends PromptContent {
    /**
     * MimeType, like "image/png"
     */
    Resource resource;

    @Data
    public static class Resource {
        /**
         * A valid resource URI
         */
        String uri;

        /**
         * The appropriate MIME type
         */
        String mimeType;

        /**
         * Either text content or base64-encoded blob data
         */
        String text;
    }
}
