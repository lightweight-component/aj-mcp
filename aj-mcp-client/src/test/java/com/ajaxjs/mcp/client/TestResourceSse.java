package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.HttpMcpTransport;
import com.ajaxjs.mcp.client.transport.McpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class TestResourceSse extends TestResourceBase {
    private static Process process;

    @BeforeAll
    static void setup() {
//        process = startServerHttp();

        McpTransport transport = HttpMcpTransport.builder()
                .sseUrl("http://localhost:8080/sse")
                .logRequests(true)
                .logResponses(true)
                .build();

        mcpClient = McpClient.builder()
                .transport(transport)
                .build();

        mcpClient.initialize();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mcpClient != null)
            mcpClient.close();

        if (process != null && process.isAlive())
            process.destroyForcibly();
    }
}
