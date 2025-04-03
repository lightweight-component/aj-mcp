package com.ajaxjs.mcp.client.integration;


import com.ajaxjs.mcp.client.DefaultMcpClient;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.stdio.StdioMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.util.Arrays;

import static com.ajaxjs.mcp.client.integration.McpServerHelper.getJBangCommand;
import static com.ajaxjs.mcp.client.integration.McpServerHelper.getPathToScript;

class McpLoggingStdioTransportIT extends McpLoggingTestBase {

    @BeforeAll
    static void setup() {
        McpTransport transport = new StdioMcpTransport.Builder()
                .command(Arrays.asList(getJBangCommand(), "--quiet", "--fresh", "run", getPathToScript("logging_mcp_server.java")))
                .logEvents(true)
                .build();
        logMessageHandler = new TestMcpLogHandler();
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
    }
}
