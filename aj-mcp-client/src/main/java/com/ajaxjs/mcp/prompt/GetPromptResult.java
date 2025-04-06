package com.ajaxjs.mcp.prompt;

import lombok.Data;

import java.util.List;

@Data
public class GetPromptResult {
    String description;

    List<PromptMessage> messages;
}
