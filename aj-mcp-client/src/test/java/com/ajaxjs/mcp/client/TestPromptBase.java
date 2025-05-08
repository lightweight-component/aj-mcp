package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.prompt.PromptItem;

import java.util.List;

public abstract class TestPromptBase {
    static IMcpClient mcpClient;

    PromptItem findPromptByName(String name, List<PromptItem> promptRefs) {
        for (PromptItem promptRef : promptRefs) {
            if (promptRef.getName().equals(name))
                return promptRef;
        }

        return null;
    }
}
