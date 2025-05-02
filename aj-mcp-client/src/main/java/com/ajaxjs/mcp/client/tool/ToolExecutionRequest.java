package com.ajaxjs.mcp.client.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents an LLM-generated request to execute a tool.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ToolExecutionRequest {
    /**
     * The id of the tool.
     */
    private String id;

    /**
     * The name of the tool.
     */
    private final String name;

    /**
     * The arguments of the tool.
     */
    private String arguments;

    public ToolExecutionRequest(String name, String arguments) {
        this(name);
        this.arguments = arguments;
    }
}
