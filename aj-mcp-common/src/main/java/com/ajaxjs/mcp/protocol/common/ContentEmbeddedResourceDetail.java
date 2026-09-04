package com.ajaxjs.mcp.protocol.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Represents content embedded resource detail.
 */
@Data
public class ContentEmbeddedResourceDetail {
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

    /**
     * Base64-encoded binary data. Exactly one of text and blob should be set.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String blob;
}