package com.ajaxjs.mcp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStdioServerPrompt extends TestStdioServerBase {
    @Test
    void testList() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"prompts/list\"}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"prompts\":[{\"name\":\"image\",\"description\":\"Prompt that returns an image\",\"arguments\":null},{\"name\":\"parametrized\",\"description\":\"Parametrized prompt\",\"arguments\":[{\"name\":\"name\",\"description\":\"The name\",\"required\":true}]},{\"name\":\"basic\",\"description\":\"Basic simple prompt\",\"arguments\":null},{\"name\":\"embeddedBinaryResource\",\"description\":\"Prompt that returns an embedded binary resource\",\"arguments\":null},{\"name\":\"multi\",\"description\":\"Prompt that returns two messages\",\"arguments\":null}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testGetPrompt() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"prompts/get\",\"params\":{\"name\":\"basic\"}}\n");

        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"description\":\"Basic simple prompt\",\"messages\":[{\"role\":\"USER\",\"content\":{\"type\":\"text\",\"text\":\"Hello, how are you?\"}}]}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }
}
