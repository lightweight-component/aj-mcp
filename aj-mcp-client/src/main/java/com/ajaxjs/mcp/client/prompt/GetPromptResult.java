package com.ajaxjs.mcp.client.prompt;

import lombok.Data;

import java.util.List;

@Data
public class GetPromptResult {
    String description;

    List<PromptMessage> messages;
}
