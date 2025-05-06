package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStdioServerInitialize extends TestStdioServerBase {
    @Test
    void testErr() {
        setIn("{\"jsonrpc\": \"1.0\"}\n");
        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid jsonrpc version: 1.0\"}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testPing() {
        setIn("{\"jsonrpc\": \"2.0\",\"id\": 1,\"method\": \"ping\"}\n");

        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testPing2() {
        PingRequest pingRequest = new PingRequest();
        pingRequest.setId(1L);
        String json = JsonUtils.toJson(pingRequest) + "\n";
        setIn(json);

        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    @Test
    void testInitialize() {
        setIn("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{\"roots\":{\"listChanged\":false}},\"clientInfo\":{\"name\":\"aj-mcp-client\",\"version\":\"1.0\"}}}\n");

        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{\"protocolVersion\":\"2024-11-05\",\"capabilities\":{\"tools\":{\"listChanged\":true},\"logging\":{}},\"serverInfo\":{\"name\":\"AJ_MCP_Server\",\"version\":\"1.0\"}}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    public static void main(String[] args) {
        callServer();
    }
}
