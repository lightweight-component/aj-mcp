package com.foo.streamable.features;

import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.prompt.*;
import com.ajaxjs.mcp.server.feature.annotation.*;

@McpService
public class DemoPrompts {
    @Prompt(description = "Asks for a short explanation")
    public PromptMessage explain(@PromptArg(value = "topic", description = "Topic to explain") String topic) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER); message.setContent(new ContentText("Briefly explain " + topic));

        return message;
    }
}
