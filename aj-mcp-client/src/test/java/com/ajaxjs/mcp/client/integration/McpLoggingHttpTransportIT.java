package com.ajaxjs.mcp.client.integration;

import com.ajaxjs.mcp.client.DefaultMcpClient;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static com.ajaxjs.mcp.client.integration.McpServerHelper.skipTestsIfJbangNotAvailable;
import static com.ajaxjs.mcp.client.integration.McpServerHelper.startServerHttp;

class McpLoggingHttpTransportIT extends McpLoggingTestBase {

    private static final Logger log = LoggerFactory.getLogger(McpLoggingHttpTransportIT.class);
    private static Process process;

    @BeforeAll
    static void setup() throws IOException, InterruptedException, TimeoutException {
        skipTestsIfJbangNotAvailable();
        process = startServerHttp("logging_mcp_server.java");
        logMessageHandler = new TestMcpLogHandler();
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:8080/mcp/sse")
                .logRequests(true)
                .logResponses(true)
                .build();
        mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .toolExecutionTimeout(Duration.ofSeconds(4))
                .logHandler(logMessageHandler)
                .build();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (mcpClient != null) {
            mcpClient.close();
        }
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }
}
