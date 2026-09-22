package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.tools.ToolItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * Aggregates tools exposed by one or more MCP clients.
 *
 * <p>Each discovered tool is associated with the client that supplied it, so
 * invoking the returned function routes the call back to the correct server.
 * If multiple servers expose equal tool metadata, normal map replacement rules
 * apply. Tool discovery is performed each time {@link #provideTools()} is
 * called; this class does not subscribe to list-change notifications.</p>
 *
 * <p>Usage:
 * Function{CallToolRequest, String} executor = obtainTools().findToolExecutorByName("echoString");
 * String toolExecutionResultString = executor.apply(new CallToolRequest("echoString", "{\"input\": \"hi\"}"));
 */
@Slf4j
@Data
public class McpToolProvider {
    /**
     * The list of MCP clients to use for retrieving tools.
     */
    private List<IMcpClient> mcpClients;

    /**
     * If this is true, then the tool provider will throw an exception if it fails to list tools from any of the servers.
     * If this is false (default), then the tool provider will ignore the error and continue with the next server.
     */
    private boolean failIfOneServerFails;

    /**
     * Configures a single MCP client as the provider source.
     *
     * @param client client from which tools will be discovered
     */
    public void setMcpClient(IMcpClient client) {
        setMcpClients(Collections.singletonList(client));
    }

    /**
     * Discovers tools from every configured client and binds an executor to each.
     *
     * <p>When {@code failIfOneServerFails} is false, a failing client is
     * logged and discovery continues. The returned result is never null.</p>
     *
     * @return aggregated tools and their client-bound executors
     * @throws RuntimeException if discovery fails and failIfOneServerFails is true
     */
    public McpToolProviderResult provideTools() {
        McpToolProviderResult toolProviderResult = new McpToolProviderResult();

        for (IMcpClient mcpClient : mcpClients) {
            try {
                List<ToolItem> tools = mcpClient.listTools();

                for (ToolItem tool : tools)
                    toolProviderResult.put(tool, mcpClient::callTool);
            } catch (Exception e) {
                if (failIfOneServerFails)
                    throw new RuntimeException("Failed to retrieve tools from MCP server", e);
                else
                    log.warn("Failed to retrieve tools from MCP server", e);
            }
        }

        return toolProviderResult;
    }
}
