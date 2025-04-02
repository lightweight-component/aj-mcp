package com.ajaxjs.mcp.prompt;

import lombok.Data;

/**
 * The 'PromptArgument' object from the MCP protocol schema.
 */
@Data
public class PromptArgument {
    String name;
    String description;
    boolean required;
}
