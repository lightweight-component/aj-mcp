package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;

/**
 * The 'PromptArgument' object from the MCP protocol schema.
 */
@Data
public class PromptArgument {
    /**
     * Holds the name value.
     */
    String name;

    /**
     * Holds the description value.
     */
    String description;

    /**
     * Holds the required value.
     */
    boolean required;
}
