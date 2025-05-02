package com.ajaxjs.mcp.client.tool;


/**
 * Represents the result of a tool execution in response to a {@link ToolExecutionRequest}.
 * {@link ToolExecutionRequest}s come from a previous {@link AiMessage#toolExecutionRequests()}.
 */
public class ToolExecutionResultMessage {
    private final String id;
    private final String toolName;
    private final String text;

    /**
     * Creates a {@link ToolExecutionResultMessage}.
     *
     * @param id       the id of the tool.
     * @param toolName the name of the tool.
     * @param text     the result of the tool execution.
     */
    public ToolExecutionResultMessage(String id, String toolName, String text) {
        this.id = id;
        this.toolName = toolName;
        this.text = text;
    }

    /**
     * Returns the id of the tool.
     *
     * @return the id of the tool.
     */
    public String id() {
        return id;
    }

    /**
     * Returns the name of the tool.
     *
     * @return the name of the tool.
     */
    public String toolName() {
        return toolName;
    }

    /**
     * Returns the result of the tool execution.
     *
     * @return the result of the tool execution.
     */
    public String text() {
        return text;
    }
}
