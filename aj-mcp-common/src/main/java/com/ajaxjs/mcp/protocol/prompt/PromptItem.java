package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;

import java.util.List;

/**
 * Prompt item in the list
 */
@Data
public class PromptItem {
    String name;

    String description;

    List<PromptArgument> arguments;
}
