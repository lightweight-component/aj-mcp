package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.McpConstant;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Resource link content allowed in tool results since MCP 2025-06-18. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContentResourceLink extends Content {
    private String uri;
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String mimeType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long size;

    public ContentResourceLink() {
        this.type = McpConstant.ContentType.RESOURCE_LINK;
    }
}
