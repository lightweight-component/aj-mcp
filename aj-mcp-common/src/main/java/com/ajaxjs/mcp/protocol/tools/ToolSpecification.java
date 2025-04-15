package com.ajaxjs.mcp.protocol.tools;

import lombok.Data;

/**
 * Describes a tool that language model can execute.
 * <p>
 * Represents a tool that the server provides. Tools enable servers to expose executable functionality to the system.
 * Through these tools, you can interact with external systems, perform computations, and take actions in the real world.
 * <p>
 * Can be generated automatically from methods annotated with Tool using ToolSpecifications helper.
 */
@Data
public class ToolSpecification {
    /**
     * A unique identifier for the tool. This name is used when calling the tool.
     */
    private String name;

    /**
     * A human-readable description of what the tool does. This can be used by clients to improve the LLM's understanding of available tools.
     */
    private String description;

    /**
     * A JSON Schema object that describes the expected structure of the arguments when calling this tool.
     * This allows clients to validate tool arguments before sending them to the server.
     */
    private JsonSchema inputSchema;
}
