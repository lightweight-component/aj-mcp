package com.ajaxjs.mcp.client;


import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A tool provider backed by one or more MCP clients.
 * Usage:
 * Function<CallToolRequest, String> executor = obtainTools().findToolExecutorByName("echoString");
 * String toolExecutionResultString = executor.apply(new CallToolRequest("echoString", "{\"input\": \"hi\"}"));
 */
@Slf4j
@Data
@Deprecated
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

    public void setMcpClient(IMcpClient client) {
        setMcpClients(Collections.singletonList(client));
    }

    /**
     * Get the tool list from all MCP clients.
     *
     * @return tool list
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

    /**
     * Tool provider result
     */
    public static class McpToolProviderResult extends LinkedHashMap<ToolItem, Function<CallToolRequest, String>> {
        public ToolItem findToolByName(String name) {
            for (ToolItem tool : keySet()) {
                if (tool.getName().equals(name))
                    return tool;
            }

            return null;
        }

        public Function<CallToolRequest, String> findToolExecutorByName(String name) {
            for (Map.Entry<ToolItem, Function<CallToolRequest, String>> entry : entrySet()) {
                if (entry.getKey().getName().equals(name))
                    return entry.getValue();
            }

            return null;
        }
    }

}
