package com.ajaxjs.mcp.client.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class McpLogMessageTest {
    @Test
    public void testLogMessageWithoutLogger() {
        String json =
                "   {\n" +
                        "                  \"method\" : \"notifications/message\",\n" +
                        "                  \"params\" : {\n" +
                        "                    \"level\" : \"info\",\n" +
                        "                    \"data\" : \"Searching DuckDuckGo for: length of pont des arts in meters\"\n" +
                        "                  },\n" +
                        "                  \"jsonrpc\" : \"2.0\"\n" +
                        "                }";

        JsonNode jsonNode = toJsonNode(json);

        McpLogMessage message = McpLogMessageHandler.fromJson(jsonNode.get("params"));
        assertEquals(message.getLevel(), McpLogMessageHandler.from("info"));
        assertEquals(message.getData(), jsonNode.get("params").get("data"));
        assertNull(message.getLogger());
    }

    private static JsonNode toJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
