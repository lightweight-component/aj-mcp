package com.ajaxjs.mcp.protocol.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * The 'Tool' object from the MCP protocol schema.
 * For trust & safety and security, clients MUST consider tool annotations to be untrusted unless they come from trusted servers.
 */
@Data
public class ToolItem {
    /**
     * Unique identifier for the tool
     */
    String name;

    /**
     * Human-readable description of functionality
     */
    String description;

    /**
     * JSON Schema defining expected parameters
     */
    JsonSchema inputSchema;

    /**
     * Optional properties describing tool behavior
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, Object> annotations;
}
