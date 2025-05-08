package com.ajaxjs.mcp.protocol.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * The 'Tool' object from the MCP protocol schema.
 * For trust & safety and security, clients MUST consider tool annotations to be untrusted unless they come from trusted servers.
 * Describes a tool that language model can execute.
 * <p>
 * Represents a tool that the server provides. Tools enable servers to expose executable functionality to the system.
 * Through these tools, you can interact with external systems, perform computations, and take actions in the real world.
 * <p>
 * Can be generated automatically from methods annotated with Tool using ToolItem helper.
 */
@Data
public class ToolItem {
    /**
     * Unique identifier for the tool
     */
    String name;

    /**
     * Human-readable description of functionality. This can be used by clients to improve the LLM's understanding of available tools.
     */
    String description;

    /**
     * JSON Schema defining expected parameters.
     * JSON Schema object that describes the expected structure of the arguments when calling this tool.
     * This allows clients to validate tool arguments before sending them to the server.
     */
    JsonSchema inputSchema;

    /**
     * Optional properties describing tool behavior
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, Object> annotations;
}
