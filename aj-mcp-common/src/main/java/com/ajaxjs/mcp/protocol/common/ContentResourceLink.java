package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.McpConstant;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Resource link content allowed in tool results since MCP 2025-06-18.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContentResourceLink extends Content {
    /**
     * Holds the uri value.
     */
    private String uri;
    /**
     * Holds the name value.
     */
    private String name;

    /**
     * Holds the description value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    /**
     * Holds the mime type value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String mimeType;

    /**
     * Holds the size value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long size;

    /**
     * Creates a new content resource link.
     */
    public ContentResourceLink() {
        this.type = McpConstant.ContentType.RESOURCE_LINK;
    }
}
