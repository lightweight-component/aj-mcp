package com.ajaxjs.mcp.client.gitee;

import com.ajaxjs.mcp.client.McpClientBase;
import com.ajaxjs.mcp.client.IMcpClient;
import com.ajaxjs.mcp.client.transport.HttpMcpTransport;
import com.ajaxjs.mcp.client.transport.McpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.ajaxjs.mcp.client.McpServerHelper.skipTestsIfServerNotAvailable;
import static com.ajaxjs.mcp.client.McpServerHelper.startServerHttp;
import static org.junit.jupiter.api.Assertions.fail;


class TestToolSse extends TestToolBase {
    private static Process process;

    @BeforeAll
    static void setup() {
        skipTestsIfServerNotAvailable();
        process = startServerHttp();
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:8000/sse")
                .logRequests(true)
                .logResponses(true)
                .build();

        mcpClient = new McpClientBase.Builder()
                .transport(transport)
                .toolExecutionTimeout(Duration.ofSeconds(4))
                .build();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mcpClient != null)
            mcpClient.close();

        if (process != null && process.isAlive())
            process.destroyForcibly();
    }

    /**
     * Verify that the MCP client fails gracefully when the server returns a 404.
     */
    @Test
    void wrongUrl() throws Exception {
        IMcpClient badClient = null;

        try {
            McpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl("http://localhost:8000/WRONG")
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            badClient = new McpClientBase.Builder()
                    .transport(transport)
                    .toolExecutionTimeout(Duration.ofSeconds(4))
                    .build();
            fail("Expected an exception");
        } catch (Exception e) {

            // ok
        } finally {
            if (badClient != null) {
                badClient.close();
            }
        }
    }
}
