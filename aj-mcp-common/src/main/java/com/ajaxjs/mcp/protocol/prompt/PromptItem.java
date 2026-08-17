package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
