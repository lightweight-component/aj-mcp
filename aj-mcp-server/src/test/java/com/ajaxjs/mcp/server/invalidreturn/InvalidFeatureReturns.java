package com.ajaxjs.mcp.server.invalidreturn;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Prompt;
import com.ajaxjs.mcp.server.feature.annotation.Resource;
import com.ajaxjs.mcp.server.feature.annotation.Tool;

import java.util.Collections;
import java.util.List;

/**
 * Represents invalid feature returns.
 */
@McpService
public class InvalidFeatureReturns {
    @Tool
    public Object nullTool() {
        return null;
    }

    @Tool
    public List<String> invalidToolList() {
        return Collections.singletonList("not-content");
    }

    @Prompt
    public Object nullPrompt() {
        return null;
    }

    @Prompt
    public List<String> invalidPromptList() {
        return Collections.singletonList("not-a-prompt-message");
    }

    @Resource(uri = "test:///null")
    public Object nullResource() {
        return null;
    }

    @Resource(uri = "test:///invalid-list")
    public List<String> invalidResourceList() {
        return Collections.singletonList("not-resource-content");
    }
}
