package com.ajaxjs.mcp.server.advanced;

import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.prompt.PromptMessage;
import com.ajaxjs.mcp.protocol.resource.ResourceContentText;
import com.ajaxjs.mcp.protocol.tools.StructuredToolResult;
import com.ajaxjs.mcp.server.feature.annotation.*;

import java.util.Arrays;
import java.util.Collections;

@McpService
public class AdvancedFeatures {
    @Tool
    public String optional(@ToolArg String required,
                           @ToolArg(required = false) String suffix) {
        return required + (suffix == null ? "" : suffix);
    }

    @Prompt("greet")
    public PromptMessage greet(@PromptArg(value = "name", required = false) String name) {
        return null;
    }

    @com.ajaxjs.mcp.server.feature.annotation.ResourceTemplate(
            name = "user", uriTemplate = "users://{id}", mimeType = "text/plain")
    public ResourceContentText user(@ResourceTemplateArg(name = "id") String id) {
        ResourceContentText content = new ResourceContentText();
        content.setUri("users://" + id);
        content.setMimeType("text/plain");
        content.setText("user=" + id);
        return content;
    }

    @CompletePrompt("greet")
    public java.util.List<String> completeName(@CompleteArg(name = "name") String value) {
        return Arrays.asList(value + "-one", value + "-two");
    }

    @Tool(title = "Weather result", readOnlyHint = true,
            outputSchema = "{\"type\":\"object\",\"properties\":{\"temperature\":{\"type\":\"number\"}}}")
    public StructuredToolResult structured() {
        return new StructuredToolResult(
                Collections.singletonList(new ContentText("temperature=21")),
                Collections.<String, Object>singletonMap("temperature", 21), false);
    }
}
