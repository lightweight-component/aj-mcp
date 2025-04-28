package com.ajaxjs.mcp.client;


import com.ajaxjs.mcp.client.protocol.prompt.*;
import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.client.protocol.resource.BlobResourceContents;
import com.ajaxjs.mcp.client.protocol.resource.ResourceContents;
import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.prompt.Role;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TestPromptsBase {
    static IMcpClient mcpClient;

    @Test
    void getBasicPrompt() {
        GetPromptResult prompt = mcpClient.getPrompt("basic", new HashMap<>());
        assertNull(prompt.getDescription(), "Description should be null");
        assertEquals(1, prompt.getMessages().size(), "Expected exactly one message");

        PromptMessage message = prompt.getMessages().get(0);
        assertEquals(Role.USER, message.getRole(), "Role should be USER");
        assertEquals("Hello", ((PromptTextContent) message.getContent()).getText(), "Text content should be 'Hello'");
    }

    @Test
    void getMultiPrompt() {
        GetPromptResult prompt = mcpClient.getPrompt("multi", new HashMap<>());
        assertNull(prompt.getDescription(), "Description should be null");
        assertEquals(2, prompt.getMessages().size(), "Expected exactly two messages");

        PromptMessage message1 = prompt.getMessages().get(0);
        assertEquals(Role.USER, message1.getRole(), "Role of message1 should be USER");
        assertEquals(PromptContent.Type.TEXT, message1.getContent().getType(), "Type of message1 content should be TEXT");
        assertEquals("first", ((PromptTextContent) message1.getContent()).getText(), "Text content of message1 should be 'first'");

        PromptMessage message2 = prompt.getMessages().get(1);
        assertEquals(Role.USER, message2.getRole(), "Role of message2 should be USER");
        assertEquals(PromptContent.Type.TEXT, message2.getContent().getType(), "Type of message2 content should be TEXT");
        assertEquals("second", ((PromptTextContent) message2.getContent()).getText(), "Text content of message2 should be 'second'");
    }

    @Test
    void getImagePrompt() {
        GetPromptResult prompt = mcpClient.getPrompt("image", new HashMap<>());
        assertNull(prompt.getDescription(), "Description should be null");
        assertEquals(1, prompt.getMessages().size(), "Expected exactly one message");

        PromptMessage message = prompt.getMessages().get(0);
        assertEquals(Role.USER, message.getRole(), "Role should be USER");
        assertEquals(PromptContent.Type.IMAGE, message.getContent().getType(), "Content type should be IMAGE");

        PromptImageContent imageContent = (PromptImageContent) message.getContent();
        assertEquals("aaa", imageContent.getData(), "Image data should be 'aaa'");
        assertEquals("image/png", imageContent.getMimeType(), "Image MIME type should be 'image/png'");
    }

    @Test
    void getParametrizedPrompt() {
        GetPromptResult prompt = mcpClient.getPrompt("parametrized", McpUtils.mapOf("name", "Bob"));
        assertNull(prompt.getDescription(), "Description should be null");
        assertEquals(1, prompt.getMessages().size(), "Expected exactly one message");

        PromptMessage message = prompt.getMessages().get(0);
        assertEquals(Role.USER, message.getRole(), "Role should be USER");
        assertEquals(PromptContent.Type.TEXT, message.getContent().getType(), "Content type should be TEXT");

        PromptTextContent textContent = (PromptTextContent) message.getContent();
        assertEquals("Hello Bob", textContent.getText(), "Text content should be 'Hello Bob'");
    }

    @Test
    void getNonExistentPrompt() {
        assertThrows(McpException.class, () -> mcpClient.getPrompt("DOES-NOT-EXIST", new HashMap<>()), "Expected McpException to be thrown for non-existent prompt");
    }

    @Test
    void getPromptWithEmbeddedBinaryResource() {
        GetPromptResult prompt = mcpClient.getPrompt("embeddedBinaryResource", new HashMap<>());
        PromptMessage message = prompt.getMessages().get(0);

        assertEquals(Role.USER, message.getRole(), "Role should be USER");
        assertEquals(PromptContent.Type.RESOURCE, message.getContent().getType(), "Content type should be RESOURCE");
        assertTrue(message.getContent() instanceof EmbeddedResource, "Content should be an instance of EmbeddedResource");

        EmbeddedResource resource = (EmbeddedResource) message.getContent();
        assertEquals(ResourceContents.Type.BLOB, resource.getResource().getType(), "Resource type should be BLOB");

        BlobResourceContents blob = (BlobResourceContents) resource.getResource();
        assertEquals("file:///embedded-blob", blob.getUri(), "URI should be 'file:///embedded-blob'");
        assertEquals("aaaaa", blob.getBlob(), "Blob data should be 'aaaaa'");
        assertEquals("application/octet-stream", blob.getMimeType(), "MIME type should be 'application/octet-stream'");
    }

    private PromptItem findPromptByName(String name, List<PromptItem> promptRefs) {
        for (PromptItem promptRef : promptRefs) {
            if (promptRef.getName().equals(name))
                return promptRef;
        }

        return null;
    }
}
