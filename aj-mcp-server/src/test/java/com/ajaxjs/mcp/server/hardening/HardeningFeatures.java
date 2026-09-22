package com.ajaxjs.mcp.server.hardening;

import com.ajaxjs.mcp.server.feature.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fixtures for numeric and context-aware completion boundaries.
 */
@McpService
public class HardeningFeatures {
    @Tool
    public String integer(@ToolArg("count") int count) {
        return String.valueOf(count);
    }

    @Tool
    public String echo(@ToolArg("text") String text) {
        return text;
    }

    @CompletePrompt("framework")
    public List<String> framework(@CompleteArg(name = "framework") String value, Map<String, String> context) {
        return Collections.singletonList(context.get("language") + ":" + value);
    }
}
