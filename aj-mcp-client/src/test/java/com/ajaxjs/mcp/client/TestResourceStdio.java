package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.StdioTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.util.Arrays;

class TestResourceStdio extends TestResourceBase {
    @BeforeAll
    static void setup() {
        McpTransport transport = StdioTransport.builder()
                .command(Arrays.asList("C:\\ai\\gitee\\mcp-gitee.exe", "-token", "2da9aaae1a086618ec1bb576479c8780"))
                .logEvents(true)
                .build();

        mcpClient = new McpClient.Builder()
                .transport(transport)
                .toolExecutionTimeout(Duration.ofSeconds(4))
                .build();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mcpClient != null)
            mcpClient.close();
    }
}
