package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.prompt.PromptArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
