package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;

import java.util.Map;

@Data
public class GetPromptRequestParams {
    private String name;

    private Map<String, Object> arguments;
}