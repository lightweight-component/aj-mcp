package com.ajaxjs.mcp.protocol.resource;

import lombok.Data;

/**
 * Resources can contain either text or binary data
 */
@Data
public abstract class ResourceContent extends com.ajaxjs.mcp.protocol.common.Metadata {
    /**
     * Unique identifier for the resource
     */
    private String uri;

    /**
     * MIME type
     */
    private String mimeType;
}
