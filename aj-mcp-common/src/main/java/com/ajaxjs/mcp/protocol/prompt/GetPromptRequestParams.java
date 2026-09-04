package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;

import java.util.Map;

/**
 * Represents get prompt request params.
 */
@Data
public class GetPromptRequestParams {
    /**
     * Holds the name value.
     */
    private String name;

    /**
     * Holds the arguments value.
     */
    private Map<String, Object> arguments;
}