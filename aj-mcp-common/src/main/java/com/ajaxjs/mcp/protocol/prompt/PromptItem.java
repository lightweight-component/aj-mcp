package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Prompt item in the list
 */
@Data
public class PromptItem {
    String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String title;

    String description;

    List<PromptArgument> arguments;
}
