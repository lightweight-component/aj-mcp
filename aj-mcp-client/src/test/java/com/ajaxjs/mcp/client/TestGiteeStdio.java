package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.tool.McpToolProvider;
import com.ajaxjs.mcp.client.tool.ToolExecutor;
import com.ajaxjs.mcp.client.tool.ToolProviderResult;
import com.ajaxjs.mcp.client.tool.ToolSpecification;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.StdioTransport;
import com.ajaxjs.mcp.message.ToolExecutionRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestGiteeStdio extends TestGiteeBase {
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
    static void teardown() throws Exception {
        if (mcpClient != null)
            mcpClient.close();
    }
}
