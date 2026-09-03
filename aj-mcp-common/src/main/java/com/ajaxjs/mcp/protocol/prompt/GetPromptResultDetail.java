package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;

import java.util.List;

@Data
public class GetPromptResultDetail {
    String description;

    List<PromptMessage> messages;
}