package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.StdioTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestInitializeStdio extends TestInitializeBase {
    @BeforeAll
    static void setup() {
        mcpClient = McpClient.createStdioMcpClient("java", "-jar",
                "C:\\code\\ajaxjs\\aj-mcp\\samples\\server\\server-stdio\\target\\my-app-jar-with-dependencies.jar");
    }

    @Test
    void testConfig() {
        McpTransport transport = StdioTransport.builder()
                .command(Arrays.asList("java", "-jar",
                        "C:\\code\\ajaxjs\\aj-mcp\\samples\\server\\server-stdio\\target\\my-app-jar-with-dependencies.jar"))
                .logEvents(true)
                .build();

        McpClient mcpClient = McpClient.builder()
                .clientName("my-host")
                .clientVersion("1.2")
                .transport(transport).build();
        mcpClient.initialize();

        assertNotNull(mcpClient);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mcpClient != null)
            mcpClient.close();
    }
}
