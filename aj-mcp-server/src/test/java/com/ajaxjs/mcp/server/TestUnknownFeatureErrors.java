package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUnknownFeatureErrors extends TestStdioServerBase {
    @Test
    void unknownToolReturnsInvalidParams() {
        assertInvalidParams(
                "{\"jsonrpc\":\"2.0\",\"id\":41,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"missing-tool\"}}\n",
                41,
                "Unknown tool: missing-tool");
    }

    @Test
    void unknownPromptReturnsInvalidParams() {
        assertInvalidParams(
                "{\"jsonrpc\":\"2.0\",\"id\":42,\"method\":\"prompts/get\","
                        + "\"params\":{\"name\":\"missing-prompt\"}}\n",
                42,
                "Unknown prompt: missing-prompt");
    }

    @Test
    void unknownResourceReturnsInvalidParams() {
        assertInvalidParams(
                "{\"jsonrpc\":\"2.0\",\"id\":43,\"method\":\"resources/read\","
                        + "\"params\":{\"uri\":\"file:///missing\"}}\n",
                43,
                "Unknown resource: file:///missing");
    }

    private void assertInvalidParams(String request, long expectedId, String expectedMessage) {
        setIn(request);

        JsonNode response = JsonUtils.json2Node(testOut.toString());
        assertEquals(expectedId, response.get("id").asLong());
        assertEquals(-32602, response.get("error").get("code").asInt());
        assertEquals(expectedMessage, response.get("error").get("message").asText());
    }
}
