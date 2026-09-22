package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * The 'PromptArgument' object from the MCP protocol schema.
 */
@Data
public class PromptArgument {
    /**
     * Optional display label; retained when reading June 2025 prompt definitions.
     */
    @JsonInclude(NON_NULL)
    private String title;

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
