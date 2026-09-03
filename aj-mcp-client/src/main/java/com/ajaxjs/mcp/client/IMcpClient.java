package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.client.*;
import com.ajaxjs.mcp.protocol.prompt.GetPromptResult;
import com.ajaxjs.mcp.protocol.prompt.GetPromptResultDetail;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.resource.GetResourceResult;
import com.ajaxjs.mcp.protocol.resource.GetResourceResultDetail;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.CallToolResult;
import com.ajaxjs.mcp.protocol.tools.CallToolResultDetail;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.ajaxjs.mcp.protocol.utils.completion.CompleteRequest;
import com.ajaxjs.mcp.protocol.utils.completion.CompleteResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a client that can communicate with an MCP server over a given transport protocol, retrieve and execute tools using the server.
 */
public interface IMcpClient extends AutoCloseable {
    /**
     * Initializes the client by sending an initialization request to the MCP server.
     */
    void initialize();

    /**
     * Obtains a list of tools from the MCP server.
     *
     * @return The list of tools
     */
    List<ToolItem> listTools();

    /**
     * Obtains a pagination list of tools from the MCP server.
     *
     * @param pageNo The page number to retrieve.
     * @return The list of tools
     */
    List<ToolItem> listTools(int pageNo);

    McpPage<ToolItem> listToolPage(String cursor);

    /**
     * Calls a tool on the MCP server and returns the result as a String.
     *
     * @param request The tool request
     * @return The tool result
     */
    String callTool(CallToolRequest request);

    /**
     * Returns the complete result, including 2025-06-18 structured content.
     */
    CallToolResultDetail callToolResult(CallToolRequest request);

    /***
     * Calls a tool on the MCP server and returns the result as a String.
     *
     * @param name The name of the tool to call
     * @param arguments The arguments to pass to the tool
     * @return The tool result
     */
    String callTool(String name, String arguments);

    /**
     * Obtains the full list of resources available on the MCP server.
     *
     * @return The list of resources
     */
    List<ResourceItem> listResources();

    /**
     * Obtains the pagination list of resources available on the MCP server.
     *
     * @param pageNo The page number to retrieve.
     * @return The list of resources
     */
    List<ResourceItem> listResources(int pageNo);

    McpPage<ResourceItem> listResourcePage(String cursor);

    /**
     * Obtains the full list of resource templates (dynamic resources) available on the MCP server.
     *
     * @return The list of resource templates
     */
    List<ResourceTemplate> listResourceTemplates();

    /**
     * Obtains the pagination list of resource templates (dynamic resources) available on the MCP server.
     *
     * @param pageNo The page number to retrieve.
     * @return The list of resource templates
     */
    List<ResourceTemplate> listResourceTemplates(int pageNo);

    McpPage<ResourceTemplate> listResourceTemplatePage(String cursor);

    /**
     * Retrieves the contents of the resource with the specified URI.
     * This also works for dynamic resources (templates).
     *
     * @param uri The URI of the resource to retrieve.
     * @return Resource contents.
     */
    GetResourceResultDetail readResource(String uri);

    void subscribeResource(String uri);

    void unsubscribeResource(String uri);

    /**
     * Obtain a list of prompts available on the MCP server.
     *
     * @return The list of prompts
     */
    List<PromptItem> listPrompts();

    /**
     * Obtain a pagination list of prompts available on the MCP server.
     *
     * @param pageNo The page number to retrieve.
     * @return The list of prompts
     */
    List<PromptItem> listPrompts(int pageNo);

    McpPage<PromptItem> listPromptPage(String cursor);

    /**
     * Render the contents of a prompt.
     *
     * @param name      The name of the prompt to render.
     * @param arguments The arguments to pass to the prompt.
     * @return The prompt result.
     */
    GetPromptResultDetail getPrompt(String name, Map<String, Object> arguments);

    /**
     * Render the contents of a prompt.
     *
     * @param name      The name of the prompt to render.
     * @param arguments The arguments to pass to the prompt.
     * @return The prompt result.
     */
    GetPromptResultDetail getPrompt(String name, String arguments);

    /**
     * Performs a health check that returns normally if the MCP server is reachable and
     * properly responding to ping requests. If this method throws an exception,
     * the health of this MCP client is considered degraded.
     */
    void checkHealth();

    /**
     * Requests argument completion for a prompt or resource template.
     */
    CompleteResult.CompletionResult complete(CompleteRequest.Ref ref, CompleteRequest.Argument argument);

    /**
     * 2025-06-18 completion request with previously resolved arguments.
     */
    CompleteResult.CompletionResult complete(CompleteRequest.Ref ref, CompleteRequest.Argument argument,
                                             Map<String, String> context);

    /**
     * Registers an observer for a JSON-RPC notification method.
     */
    void onNotification(String method, Consumer<JsonNode> handler);

    /**
     * Registers a handler for a server-initiated JSON-RPC request such as roots/list or sampling/createMessage.
     */
    void onServerRequest(String method, Function<JsonNode, JsonNode> handler);

    void setRoots(List<Root> roots, boolean notifyChanges);

    void notifyRootsChanged();

    void setSamplingHandler(Function<SamplingCreateMessageParams, SamplingCreateMessageResult> handler);

    /**
     * Registers the user-interaction handler advertised by MCP 2025-06-18 clients.
     */
    void setElicitationHandler(Function<ElicitRequestParams, ElicitResult> handler);

    /**
     * Returns the protocol revision selected during initialization.
     */
    String getNegotiatedProtocolVersion();
}
