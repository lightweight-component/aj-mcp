package com.ajaxjs.mcp.client.tool;


import com.ajaxjs.mcp.client.IMcpClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * A tool provider backed by one or more MCP clients.
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

    public void setMcpClient(IMcpClient client) {
        setMcpClients(Collections.singletonList(client));
    }

    /**
     * Get the tool list from all MCP clients.
     *
     * @param request
     * @return tool list
     */
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult toolProviderResult = new ToolProviderResult();

        for (IMcpClient mcpClient : mcpClients) {
            try {
                List<ToolSpecification> toolSpecifications = mcpClient.listTools();

                for (ToolSpecification toolSpecification : toolSpecifications)
                    toolProviderResult.put(toolSpecification, (executionRequest, memoryId) -> mcpClient.executeTool(executionRequest));
            } catch (Exception e) {
                if (failIfOneServerFails)
                    throw new RuntimeException("Failed to retrieve tools from MCP server", e);
                else
                    log.warn("Failed to retrieve tools from MCP server", e);
            }
        }

        return toolProviderResult;
    }

    public ToolProviderResult provideTools() {
        return provideTools(null);
    }
}
