package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.prompt.PromptArgument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TestPromptBase {
    static IMcpClient mcpClient;

    @Test
    void listPrompts() {
        List<PromptItem> prompts = mcpClient.listPrompts();
        assertEquals(5, prompts.size(), "Expected 5 prompts");

        PromptItem basic = findPromptByName("basic", prompts);
        assertNotNull(basic, "Prompt 'basic' should not be null");
        assertEquals("Basic simple prompt", basic.getDescription(), "Description mismatch for 'basic'");
        assertTrue(basic.getArguments().isEmpty(), "'basic' arguments should be empty");

        PromptItem image = findPromptByName("image", prompts);
        assertNotNull(image, "Prompt 'image' should not be null");
        assertEquals("Prompt that returns an image", image.getDescription(), "Description mismatch for 'image'");
        assertTrue(image.getArguments().isEmpty(), "'image' arguments should be empty");

        PromptItem multi = findPromptByName("multi", prompts);
        assertNotNull(multi, "Prompt 'multi' should not be null");
        assertEquals("Prompt that returns two messages", multi.getDescription(), "Description mismatch for 'multi'");
        assertTrue(multi.getArguments().isEmpty(), "'multi' arguments should be empty");

        PromptItem parametrized = findPromptByName("parametrized", prompts);
        assertNotNull(parametrized, "Prompt 'parametrized' should not be null");
        assertEquals("Parametrized prompt", parametrized.getDescription(), "Description mismatch for 'parametrized'");
        assertEquals(1, parametrized.getArguments().size(), "'parametrized' should have exactly one argument");
        PromptArgument arg = parametrized.getArguments().get(0);
        assertEquals("name", arg.getName(), "Argument name mismatch");
        assertEquals("The name", arg.getDescription(), "Argument description mismatch");

        PromptItem embeddedBlob = findPromptByName("embeddedBinaryResource", prompts);
        assertNotNull(embeddedBlob, "Prompt 'embeddedBinaryResource' should not be null");
        assertEquals("Prompt that returns an embedded binary resource", embeddedBlob.getDescription(), "Description mismatch for 'embeddedBinaryResource'");
        assertTrue(embeddedBlob.getArguments().isEmpty(), "'embeddedBinaryResource' arguments should be empty");
    }

    private PromptItem findPromptByName(String name, List<PromptItem> promptRefs) {
        for (PromptItem promptRef : promptRefs) {
            if (promptRef.getName().equals(name))
                return promptRef;
        }

        return null;
    }
}
