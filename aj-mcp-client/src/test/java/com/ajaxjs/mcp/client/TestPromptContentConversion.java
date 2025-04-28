package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.protocol.prompt.GetPromptResult;
import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/**
 * Test for converting PromptMessage as returned from MCP servers to instances of ChatMessage from the core langchain4j API.
 */
public class TestPromptContentConversion {
    @Test
    public void testUserMessageWithText() {
        // language=JSON
        String response = "{\"jsonrpc\":\"2.0\",\"id\":111,\"result\":{\"messages\":[{\"role\":\"user\",\"content\":{\"text\":\"Hello\",\"type\":\"text\"}}]}}";
        JsonNode responseJsonNode = JsonUtils.json2Node(response);
        GetPromptResult promptResponse = ClientPrompt.parsePromptContents(responseJsonNode);

//        ChatMessage chatMessage = promptResponse.getMessages().get(0).toChatMessage();
//        assertInstanceOf(UserMessage.class, chatMessage, "ChatMessage should be an instance of UserMessage");
//        assertEquals("Hello", ((UserMessage) chatMessage).singleText(), "The text content should be 'Hello'");
    }

    @Test
    public void testAiMessageWithText() {
        // language=JSON
        String response = "{\"jsonrpc\":\"2.0\",\"id\":123,\"result\":{\"messages\":[{\"role\":\"assistant\",\"content\":{\"text\":\"Hello\",\"type\":\"text\"}}]}}";
        JsonNode responseJsonNode = JsonUtils.json2Node(response);
        GetPromptResult promptResponse = ClientPrompt.parsePromptContents(responseJsonNode);

//        ChatMessage chatMessage = promptResponse.getMessages().get(0).toChatMessage();
//        assertInstanceOf(AiMessage.class, chatMessage, "ChatMessage should be an instance of AiMessage");
//        assertEquals("Hello", ((AiMessage) chatMessage).text(), "The text content should be 'Hello'");
    }

    @Test
    public void testUserMessageWithImage() {
        // language=JSON
        String response = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"messages\":[{\"role\":\"user\",\"content\":{\"data\":\"aaa\",\"mimeType\":\"image/png\",\"type\":\"image\"}}]}}";
        JsonNode responseJsonNode = JsonUtils.json2Node(response);
        GetPromptResult promptResponse = ClientPrompt.parsePromptContents(responseJsonNode);

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
