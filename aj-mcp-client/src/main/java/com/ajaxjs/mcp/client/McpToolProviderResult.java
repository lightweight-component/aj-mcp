package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.ToolItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Tool provider result
 */
public class McpToolProviderResult extends LinkedHashMap<ToolItem, Function<CallToolRequest, String>> {
    /**
     * Executes the find tool by name operation.
     *
     * @param name the name value.
     * @return the result of the find tool by name operation.
     */
    public ToolItem findToolByName(String name) {
        for (ToolItem tool : keySet()) {
            if (tool.getName().equals(name))
                return tool;
        }

        return null;
    }

    /**
     * Executes the find tool executor by name operation.
     *
     * @param name the name value.
     * @return the result of the find tool executor by name operation.
     */
    public Function<CallToolRequest, String> findToolExecutorByName(String name) {
        for (Map.Entry<ToolItem, Function<CallToolRequest, String>> entry : entrySet()) {
            if (entry.getKey().getName().equals(name))
                return entry.getValue();
        }

        return null;
    }
}