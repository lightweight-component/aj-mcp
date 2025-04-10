package com.ajaxjs.mcp.client.prompt;

import lombok.Data;

import java.util.List;

@Data
public class Prompt {
    String name;

    String description;

    List<PromptArgument> arguments;
}
