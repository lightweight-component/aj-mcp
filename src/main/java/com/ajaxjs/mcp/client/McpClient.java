package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.prompt.GetPromptResult;
import com.ajaxjs.mcp.prompt.Prompt;
import com.ajaxjs.mcp.resource.ReadResourceResult;
import com.ajaxjs.mcp.resource.Resource;
import com.ajaxjs.mcp.resource.ResourceTemplate;
import com.ajaxjs.mcp.tool.ToolExecutionRequest;
import com.ajaxjs.mcp.tool.ToolSpecification;

import java.util.List;
import java.util.Map;

/**
 * Represents a client that can communicate with an MCP server over a given transport protocol,
 * retrieve and execute tools using the server.
 */
public interface McpClient extends AutoCloseable {
    /**
     * Obtains a list of tools from the MCP server.
     */
    List<ToolSpecification> listTools();

    /**
     * Executes a tool on the MCP server and returns the result as a String.
     * Currently, this expects a tool execution to only contain text-based results.
     */
    String executeTool(ToolExecutionRequest executionRequest);

    /**
     * Obtains the current list of resources available on the MCP server.
     */
    List<Resource> listResources();

    /**
     * Obtains the current list of resource templates (dynamic resources) available on the MCP server.
     */
    List<ResourceTemplate> listResourceTemplates();

    /**
     * Retrieves the contents of the resource with the specified URI. This also
     * works for dynamic resources (templates).
     */
    ReadResourceResult readResource(String uri);

    /**
     * Obtain a list of prompts available on the MCP server.
     */
    List<Prompt> listPrompts();

    /**
     * Render the contents of a prompt.
     */
    GetPromptResult getPrompt(String name, Map<String, Object> arguments);
}
