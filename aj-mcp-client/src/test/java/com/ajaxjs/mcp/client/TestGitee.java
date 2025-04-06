package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.logging.DefaultMcpLogMessageHandler;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.stdio.StdioTransport;
import com.ajaxjs.mcp.tool.ToolExecutionRequest;
import com.ajaxjs.mcp.tool.ToolExecutor;
import com.ajaxjs.mcp.tool.ToolProviderResult;
import com.ajaxjs.mcp.tool.ToolSpecification;
import com.ajaxjs.util.ObjectHelper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestGitee {
    McpTransport transport = StdioTransport.builder()
            .command(Arrays.asList("C:\\ai\\gitee\\mcp-gitee.exe", "-token", "2da9aaae1a086618ec1bb576479c8781"))
            .logEvents(true)
            .build();

    McpClient mcpClient = new DefaultMcpClient.Builder()
            .transport(transport)
            .logHandler(new DefaultMcpLogMessageHandler())
            .build();
    @Test
    void verifyToolSpecifications() {
        McpToolProvider toolProvider = new McpToolProvider();
        toolProvider.setMcpClients(Collections.singletonList(mcpClient));

        ToolProviderResult toolProviderResult = toolProvider.provideTools(null);

        ToolSpecification listUserRepos = findToolSpecificationByName(toolProviderResult, "list_user_repos");
        assertNotNull(listUserRepos, "Tool 'list_user_repos' should not be null");
    }

    @Test
    public void executeTool() {
        McpToolProvider toolProvider = new McpToolProvider();
        toolProvider.setMcpClients(Collections.singletonList(mcpClient));

        ToolProviderResult toolProviderResult = toolProvider.provideTools(null);
        ToolExecutor executor = findToolExecutorByName(toolProviderResult, "list_user_repos");

        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name("list_user_repos")
//                .arguments("{\"input\": \"abc\"}")
                .build();

        String toolExecutionResultString = executor.execute(toolExecutionRequest, null);

        System.out.println(toolExecutionResultString);
//        assertEquals("abc", toolExecutionResultString);
    }

    ToolSpecification findToolSpecificationByName(ToolProviderResult toolProviderResult, String name) {
        return toolProviderResult.keySet().stream()
                .filter(toolSpecification -> toolSpecification.getName().equals(name))
                .findFirst()
                .get();
    }

    ToolExecutor findToolExecutorByName(ToolProviderResult toolProviderResult, String name) {
        return toolProviderResult.entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(name))
                .findFirst()
                .get()
                .getValue();
    }
}
