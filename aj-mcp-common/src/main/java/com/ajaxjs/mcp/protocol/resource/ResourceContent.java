package com.ajaxjs.mcp.protocol.resource;

import lombok.Data;

/**
 * Resources can contain either text or binary data
 */
@Data
public abstract class ResourceContent {
    /**
     * Unique identifier for the resource
     */
    String uri;

    /**
     * MIME type
     */
    String mimeType;
}
