package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Prompt item in the list
 */
@Data
public class PromptItem {
    /**
     * Holds the name value.
     */
    String name;

    /**
     * Holds the title value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String title;

    /**
     * Holds the description value.
     */
    String description;

    /**
     * Holds the arguments value.
     */
    List<PromptArgument> arguments;
}
