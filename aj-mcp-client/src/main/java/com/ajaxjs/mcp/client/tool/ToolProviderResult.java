package com.ajaxjs.mcp.client.tool;

import com.ajaxjs.mcp.protocol.tools.ToolSpecification;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool provider result
 */
public class ToolProviderResult extends LinkedHashMap<ToolSpecification, ToolExecutor> {
    public ToolSpecification findToolSpecificationByName(String name) {
        for (ToolSpecification toolSpecification : keySet()) {
            if (toolSpecification.getName().equals(name))
                return toolSpecification;
        }

        return null;
    }

    public ToolExecutor findToolExecutorByName(String name) {
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : entrySet()) {
            if (entry.getKey().getName().equals(name))
                return entry.getValue();
        }

        return null;
    }
}
