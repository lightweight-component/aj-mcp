package com.ajaxjs.mcp.client.tool;

import com.ajaxjs.mcp.message.ToolExecutionRequest;

/**
 * A low-level executor/handler of a {@link ToolExecutionRequest}.
 */
@FunctionalInterface
public interface ToolExecutor {
    /**
     * Executes a tool requests.
     *
     * @param toolExecutionRequest The tool execution request. Contains tool name and arguments.
     * @param memoryId             The ID of the chat memory.
     * @return The result of the tool execution.
     */
    String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId);
}
