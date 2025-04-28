package com.ajaxjs.mcp.client.protocol.prompt;

import com.ajaxjs.mcp.message.Content;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * A holder for one of ['McpTextContent', 'McpImageContent', 'McpEmbeddedResource'] objects from the MCP protocol schema.
 */
public interface PromptContent {
    @JsonProperty("type")
    default String getType() {
        return type().toString().toLowerCase(Locale.ROOT);
    }

    Type type();

    Content toContent();

    enum Type {
        TEXT,
        RESOURCE,
        IMAGE
    }
}
