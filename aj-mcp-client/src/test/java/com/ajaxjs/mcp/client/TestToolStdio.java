package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.StdioTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;

class TestToolStdio extends TestToolBase {
    @BeforeAll
    static void setup() {
        McpTransport transport = StdioTransport.builder()
                .command(Arrays.asList("java", "-jar", "C:\\code\\ajaxjs\\aj-mcp\\samples\\server\\server-stdio\\target\\my-app-jar-with-dependencies.jar"))
                .logEvents(true)
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
    }
}
