package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.ToolItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Ordered mapping of discovered tool metadata to executable client functions.
 *
 * <p>The function accepts a {@link CallToolRequest} and returns the legacy
 * string representation produced by its owning client. Lookup methods return
 * {@code null} when no tool with the requested name exists; callers that need
 * a mandatory tool should check explicitly and report a useful error.</p>
 */
public class McpToolProviderResult extends LinkedHashMap<ToolItem, Function<CallToolRequest, String>> {
    /**
     * Finds the metadata for a tool by its MCP name.
     *
     * @param name exact MCP tool name
     * @return matching metadata, or {@code null} when absent
     */
    public ToolItem findToolByName(String name) {
        for (ToolItem tool : keySet()) {
            if (tool.getName().equals(name))
                return tool;
        }

        return null;
    }

    /**
     * Finds the client-bound executor for a tool by its MCP name.
     *
     * @param name exact MCP tool name
     * @return executor for the matching tool, or {@code null} when absent
     */
    public Function<CallToolRequest, String> findToolExecutorByName(String name) {
        for (Map.Entry<ToolItem, Function<CallToolRequest, String>> entry : entrySet()) {
            if (entry.getKey().getName().equals(name))
                return entry.getValue();
        }

        return null;
    }
}