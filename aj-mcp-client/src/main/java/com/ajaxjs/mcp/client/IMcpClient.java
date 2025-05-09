package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.prompt.GetPromptResult;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.resource.GetResourceResult;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.ToolItem;

import java.util.List;
import java.util.Map;

/**
 * Represents a client that can communicate with an MCP server over a given transport protocol, retrieve and execute tools using the server.
 */
public interface IMcpClient extends AutoCloseable {
    /**
     * Obtains a list of tools from the MCP server.
     *
     * @return The list of tools
     */
    List<ToolItem> listTools();

    /**
     * Calls a tool on the MCP server and returns the result as a String.
     *
     * @param request The tool request
     * @return The tool result
     */
    String callTool(CallToolRequest request);

    /***
     * Calls a tool on the MCP server and returns the result as a String.
     *
     * @param name The name of the tool to call
     * @param arguments The arguments to pass to the tool
     * @return The tool result
     */
    String callTool(String name, String arguments);

    /**
     * Obtains the current list of resources available on the MCP server.
     *
     * @return The list of resources
     */
    List<ResourceItem> listResources();

    /**
     * Obtains the current list of resource templates (dynamic resources) available on the MCP server.
     *
     * @return The list of resource templates
     */
    List<ResourceTemplate> listResourceTemplates();

    /**
     * Retrieves the contents of the resource with the specified URI.
     * This also works for dynamic resources (templates).
     *
     * @param uri The URI of the resource to retrieve.
     * @return Resource contents.
     */
    GetResourceResult.ResourceResultDetail readResource(String uri);

    /**
     * Obtain a list of prompts available on the MCP server.
     *
     * @return The list of prompts
     */
    List<PromptItem> listPrompts();

    /**
     * Render the contents of a prompt.
     *
     * @param name      The name of the prompt to render.
     * @param arguments The arguments to pass to the prompt.
     * @return The prompt result.
     */
    GetPromptResult.PromptResultDetail getPrompt(String name, Map<String, Object> arguments);

    /**
     * Render the contents of a prompt.
     *
     * @param name      The name of the prompt to render.
     * @param arguments The arguments to pass to the prompt.
     * @return The prompt result.
     */
    GetPromptResult.PromptResultDetail getPrompt(String name, String arguments);

    /**
     * Performs a health check that returns normally if the MCP server is reachable and
     * properly responding to ping requests. If this method throws an exception,
     * the health of this MCP client is considered degraded.
     */
    void checkHealth();
}
