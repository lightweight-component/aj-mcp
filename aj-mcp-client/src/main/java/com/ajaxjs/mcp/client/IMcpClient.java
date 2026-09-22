package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.client.*;
import com.ajaxjs.mcp.protocol.prompt.GetPromptResultDetail;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.resource.GetResourceResultDetail;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.CallToolResultDetail;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.ajaxjs.mcp.protocol.utils.completion.CompleteRequest;
import com.ajaxjs.mcp.protocol.utils.completion.CompletionResult;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Synchronous, high-level API for communicating with an MCP server.
 *
 * <p>An implementation owns one transport and exposes MCP feature families as
 * ordinary Java methods. The transport is started and the initialization
 * handshake is completed by {@link #initialize()}; feature methods must not be
 * used before that call. Unless stated otherwise, methods block until the
 * server replies or the configured request timeout expires.</p>
 *
 * <p>List methods hide pagination where possible. Use page methods when the
 * caller needs to retain an opaque server cursor or avoid fetching all pages at
 * once. Register notification and server-request handlers before initialization
 * so their capabilities can be advertised in the handshake.</p>
 */
public interface IMcpClient extends AutoCloseable {
    /**
     * Starts the transport and performs the MCP initialization handshake.
     *
     * <p>This negotiates the protocol revision and capabilities, sends the
     * required {@code notifications/initialized} notification, and only then
     * makes the client ready for feature requests. Register handlers and roots
     * before calling this method. A failed or timed-out initialization closes
     * the transport.</p>
     *
     * @throws RuntimeException if the transport cannot start, negotiation fails,
     *                          or the server does not answer before the configured timeout
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

    /**
     * Retrieves one page of tools using an opaque server cursor.
     *
     * <p>Pass {@code null} to request the first page. The returned cursor must
     * be treated as opaque and passed unchanged to the next invocation; an
     * absent cursor means that the server has no more results.</p>
     *
     * @param cursor opaque cursor from a previous page, or {@code null} for the first page
     * @return the page items and the cursor for the next page
     */
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
     *
     * @param request the tool invocation request.
     * @return the complete tool result.
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

    /**
     * Retrieves one page of resources using an opaque server cursor.
     *
     * @param cursor opaque cursor from a previous page, or {@code null} for the first page
     * @return the page items and the cursor for the next page
     */
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

    /**
     * Retrieves one page of resource templates using an opaque server cursor.
     *
     * @param cursor opaque cursor from a previous page, or {@code null} for the first page
     * @return the page items and the cursor for the next page
     */
    McpPage<ResourceTemplate> listResourceTemplatePage(String cursor);

    /**
     * Retrieves the contents of the resource with the specified URI.
     * This also works for dynamic resources (templates).
     *
     * @param uri The URI of the resource to retrieve.
     * @return Resource contents.
     */
    GetResourceResultDetail readResource(String uri);

    /**
     * Executes the subscribe resource operation.
     *
     * @param uri the uri value.
     */
    void subscribeResource(String uri);

    /**
     * Executes the unsubscribe resource operation.
     *
     * @param uri the uri value.
     */
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

    /**
     * Retrieves one page of prompts using an opaque server cursor.
     *
     * @param cursor opaque cursor from a previous page, or {@code null} for the first page
     * @return the page items and the cursor for the next page
     */
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
     * Verifies both the local transport and the remote MCP connection.
     *
     * <p>The implementation first performs a transport-specific check (for
     * example, whether a stdio child process is alive) and then sends the MCP
     * {@code ping} request. A normal return indicates that the check completed;
     * it does not guarantee that a later operation will succeed.</p>
     *
     * @throws RuntimeException if the client is not initialized or either check fails
     */
    void checkHealth();

    /**
     * Requests argument completion for a prompt or resource template.
     *
     * @param ref      the prompt or resource-template reference.
     * @param argument the argument whose values should be completed.
     * @return the completion result.
     */
    CompletionResult complete(CompleteRequest.ParamsRef ref, CompleteRequest.Argument argument);

    /**
     * 2025-06-18 completion request with previously resolved arguments.
     *
     * @param ref      the prompt or resource-template reference.
     * @param argument the argument whose values should be completed.
     * @param context  the previously resolved argument values.
     * @return the completion result.
     */
    CompletionResult complete(CompleteRequest.ParamsRef ref, CompleteRequest.Argument argument, Map<String, String> context);

    /**
     * Registers or replaces a handler for a server notification.
     *
     * <p>The handler receives the notification's {@code params} node rather
     * than the complete JSON-RPC envelope. Registration before initialization
     * is recommended for list-change notifications.</p>
     *
     * @param method  JSON-RPC notification method
     * @param handler callback receiving notification parameters
     */
    void onNotification(String method, Consumer<JsonNode> handler);

    /**
     * Registers or replaces a handler for a server-initiated JSON-RPC request.
     *
     * <p>The handler receives the request's {@code params} node and must return
     * the JSON value used as the result. A {@code null} result is treated as an
     * unsupported request. Registering a handler before initialization also
     * advertises the corresponding client capability when applicable.</p>
     *
     * @param method  JSON-RPC request method, such as {@code roots/list} or
     *                {@code sampling/createMessage}
     * @param handler callback that converts request parameters into a result
     */
    void onServerRequest(String method, Function<JsonNode, JsonNode> handler);

    /**
     * Supplies the roots exposed to a server through {@code roots/list}.
     *
     * <p>The list is copied when configured. If {@code notifyChanges} is true,
     * {@link #notifyRootsChanged()} may be used after the list changes.</p>
     *
     * @param roots         roots to expose to the server
     * @param notifyChanges whether the client advertises and supports root-list change notifications
     */
    void setRoots(List<Root> roots, boolean notifyChanges);

    /**
     * Notifies the server that the configured roots list has changed.
     *
     * @throws IllegalStateException if root-list change notifications were not enabled
     */
    void notifyRootsChanged();

    /**
     * Executes the set sampling handler operation.
     *
     * @param handler the handler value.
     */
    void setSamplingHandler(Function<SamplingCreateMessageParams, SamplingCreateMessageResult> handler);

    /**
     * Registers the user-interaction handler advertised by MCP 2025-06-18 clients.
     *
     * <p>The handler is called for server {@code elicitation/create} requests.
     * It must return the decision/result that will be serialized as the JSON-RPC
     * response.</p>
     *
     * @param handler handler that resolves an elicitation request
     */
    void setElicitationHandler(Function<ElicitRequestParams, ElicitResult> handler);

    /**
     * Returns the protocol revision selected during initialization.
     *
     * @return the negotiated MCP protocol revision, or {@code null} before initialization.
     */
    String getNegotiatedProtocolVersion();
}
