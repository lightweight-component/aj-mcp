package com.ajaxjs.mcp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStdioServerPrompt extends TestStdioServerBase {
    @Test
    void testList() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\":1,\"method\":\"prompts/list\"}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid jsonrpc version: 1.0\"}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }
}
