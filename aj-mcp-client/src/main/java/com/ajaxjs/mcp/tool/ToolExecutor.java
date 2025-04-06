package com.ajaxjs.mcp.tool;

/**
 * A low-level executor/handler of a {@link ToolExecutionRequest}.
 */
public interface ToolExecutor {
    /**
     * Executes a tool requests.
     *
     * @param toolExecutionRequest The tool execution request. Contains tool name and arguments.
     * @param memoryId             The ID of the chat memory. See {@link MemoryId} for more details.
     * @return The result of the tool execution.
     */
    String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId);
}
