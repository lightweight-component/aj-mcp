package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.server.model.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

class TestStdioServerBase {
    final PrintStream originalOut = System.out;

    final ByteArrayOutputStream testOut = new ByteArrayOutputStream();

    final InputStream originalIn = System.in;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(testOut)); // Redirect System.out to testOut
    }

    void setIn(String simulatedInput) {
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        callServer();
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

    @AfterEach
    void restoreStreams() {
        // Restore original streams
        System.setOut(originalOut);
        System.setIn(originalIn);
    }
}
