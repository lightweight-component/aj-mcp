package com.ajaxjs.mcp.client.protocol.prompt;

import lombok.Data;

import java.util.List;

@Data
public class GetPromptResult {
    String description;

    List<PromptMessage> messages;
}
