package com.ajaxjs.mcp.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Represents an LLM-generated request to execute a tool.
 */
@Data
@AllArgsConstructor
@Builder
public class ToolExecutionRequest {
    /**
     * The id of the tool.
     */
    private String id;

    /**
     * The name of the tool.
     */
    private String name;

    /**
     * The arguments of the tool.
     */
    private String arguments;
}
