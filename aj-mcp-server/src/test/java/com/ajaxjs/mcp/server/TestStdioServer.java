package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.server.model.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStdioServer {
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();

    private final InputStream originalIn = System.in;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(testOut)); // Redirect System.out to testOut
    }

    void callServer() {
        McpServer server = new McpServer();
        server.setTransport(new ServerStdio(server));
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("AJ_MCP_Server");
        server.setServerConfig(serverConfig);

        server.start();
    }

    @Test
    void testErr() {
        // Simulate user input for System.in
        String simulatedInput = "{\"jsonrpc\": \"1.0\"}\n"; // Simulated user input (with newline)
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        callServer();

        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid jsonrpc version: 1.0\"}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }
    @Test
    void testPing() {
        // Simulate user input for System.in
        String simulatedInput = "{\"jsonrpc\": \"2.0\",\"id\": 1,\"method\": \"ping\"}\n"; // Simulated user input (with newline)
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        callServer();

        // Verify the output
        String expectedOutput = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}\r\n";
        assertEquals(expectedOutput, testOut.toString());
    }

    public static void main(String[] args) {

    }

    @AfterEach
    void restoreStreams() {
        // Restore original streams
        System.setOut(originalOut);
        System.setIn(originalIn);
    }
}
