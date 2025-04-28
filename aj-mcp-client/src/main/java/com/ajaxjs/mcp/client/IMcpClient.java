package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.protocol.prompt.GetPromptResult;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.client.protocol.resource.ReadResourceResult;
import com.ajaxjs.mcp.protocol.resource.Resource;
import com.ajaxjs.mcp.client.protocol.resource.ResourceTemplate;
import com.ajaxjs.mcp.protocol.tools.ToolSpecification;
import com.ajaxjs.mcp.message.ToolExecutionRequest;

import java.util.List;
import java.util.Map;

/**
 * Represents a client that can communicate with an MCP server over a given transport protocol, retrieve and execute tools using the server.
 */
public interface IMcpClient extends AutoCloseable {
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
    List<PromptItem> listPrompts();

    /**
     * Render the contents of a prompt.
     */
    GetPromptResult getPrompt(String name, Map<String, Object> arguments);

    /**
     * Performs a health check that returns normally if the MCP server is reachable and
     * properly responding to ping requests. If this method throws an exception,
     * the health of this MCP client is considered degraded.
     */
    void checkHealth();
}
