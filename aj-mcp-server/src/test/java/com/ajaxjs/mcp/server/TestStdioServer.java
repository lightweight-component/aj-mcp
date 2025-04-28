package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
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

    static void callServer() {
        McpServer server = new McpServer();
        server.setTransport(new ServerStdio(server));
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName("AJ_MCP_Server");
        serverConfig.setVersion("1.0");
        server.setServerConfig(serverConfig);

        server.start();
    }

    void setIn(String simulatedInput) {
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        callServer();
    }

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

    @AfterEach
    void restoreStreams() {
        // Restore original streams
        System.setOut(originalOut);
        System.setIn(originalIn);
    }
}
