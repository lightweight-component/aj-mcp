package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.stdio.ProcessIOHandler;
import com.ajaxjs.mcp.message.AiMessage;
import com.ajaxjs.mcp.message.ChatMessage;
import com.ajaxjs.mcp.message.UserMessage;
import com.ajaxjs.mcp.prompt.GetPromptResult;
import com.ajaxjs.mcp.prompt.PromptsHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Test for converting PromptMessage as returned from MCP servers to instances
 * of ChatMessage from the core langchain4j API.
 */
public class PromptContentConversionTest {
    @Test
    public void testUserMessageWithText() throws JsonProcessingException {
        // language=JSON
        String response = "{\"jsonrpc\":\"2.0\",\"id\":111,\"result\":{\"messages\":[{\"role\":\"user\",\"content\":{\"text\":\"Hello\",\"type\":\"text\"}}]}}";
        JsonNode responseJsonNode = ProcessIOHandler.OBJECT_MAPPER.readTree(response);
        GetPromptResult promptResponse = PromptsHelper.parsePromptContents(responseJsonNode);

//        ChatMessage chatMessage = promptResponse.getMessages().get(0).toChatMessage();
//        assertInstanceOf(UserMessage.class, chatMessage, "ChatMessage should be an instance of UserMessage");
//        assertEquals("Hello", ((UserMessage) chatMessage).singleText(), "The text content should be 'Hello'");
    }

    @Test
    public void testAiMessageWithText() throws JsonProcessingException {
        // language=JSON
        String response = "{\"jsonrpc\":\"2.0\",\"id\":123,\"result\":{\"messages\":[{\"role\":\"assistant\",\"content\":{\"text\":\"Hello\",\"type\":\"text\"}}]}}";
        JsonNode responseJsonNode = ProcessIOHandler.OBJECT_MAPPER.readTree(response);
        GetPromptResult promptResponse = PromptsHelper.parsePromptContents(responseJsonNode);

//        ChatMessage chatMessage = promptResponse.getMessages().get(0).toChatMessage();
//        assertInstanceOf(AiMessage.class, chatMessage, "ChatMessage should be an instance of AiMessage");
//        assertEquals("Hello", ((AiMessage) chatMessage).text(), "The text content should be 'Hello'");
    }

    @Test
    public void testUserMessageWithImage() throws JsonProcessingException {
        // language=JSON
        String response = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"messages\":[{\"role\":\"user\",\"content\":{\"data\":\"aaa\",\"mimeType\":\"image/png\",\"type\":\"image\"}}]}}";
        JsonNode responseJsonNode = ProcessIOHandler.OBJECT_MAPPER.readTree(response);
        GetPromptResult promptResponse = PromptsHelper.parsePromptContents(responseJsonNode);

//        ChatMessage chatMessage = promptResponse.getMessages().get(0).toChatMessage();
//        assertInstanceOf(UserMessage.class, chatMessage, "ChatMessage should be an instance of UserMessage");
//
//        Content content = ((UserMessage) chatMessage).contents().get(0);
//        assertInstanceOf(PromptImageContent.class, content, "Content should be an instance of ImageContent");
//
//        PromptImageContent imageContent = (PromptImageContent) content;
//        assertEquals("aaa", imageContent.image().base64Data(), "Base64 data should be 'aaa'");
//        assertEquals("image/png", imageContent.image().mimeType(), "MIME type should be 'image/png'");
    }
}
