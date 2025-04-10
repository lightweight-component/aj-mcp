package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.tool.McpToolProvider;
import com.ajaxjs.mcp.client.tool.ToolExecutor;
import com.ajaxjs.mcp.client.tool.ToolProviderResult;
import com.ajaxjs.mcp.client.tool.ToolSpecification;
import com.ajaxjs.mcp.message.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class TestGiteeBase {
    static IMcpClient mcpClient;

    @Test
    void testInitializeAndCapabilities() {
        McpToolProvider toolProvider = new McpToolProvider();
        toolProvider.setMcpClient(mcpClient);
    }

    @Test
    void verifyToolSpecifications() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolSpecification listUserRepos = toolProviderResult.findToolSpecificationByName("list_user_repos");

        System.out.println(listUserRepos.getDescription());
        assertNotNull(listUserRepos, "Tool 'list_user_repos' should not be null");
    }

    @Test
    public void executeTool() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = toolProviderResult.findToolExecutorByName("list_user_repos");

        String toolExecutionResultString = executor.execute(new ToolExecutionRequest("list_user_repos"), null);

        System.out.println(toolExecutionResultString);
//        assertEquals("abc", toolExecutionResultString);
    }

    @Test
    public void executeNonExistentTool() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = toolProviderResult.findToolExecutorByName("list_user_repos");

        String toolExecutionResultString = executor.execute(new ToolExecutionRequest("THIS-TOOL-DOES-NOT-EXIST", "{\"input\": 1}"), null);
        assertEquals("There was an error executing the tool. " + "Message: Tool not found: THIS-TOOL-DOES-NOT-EXIST. Code: -32602", toolExecutionResultString);
    }

    @Test
    public void executeToolWithWrongArgumentType() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = toolProviderResult.findToolExecutorByName("list_user_repos");

        String toolExecutionResultString = executor.execute(new ToolExecutionRequest("list_user_repos", "{\"input\": 1}"), null);
        assertEquals("There was an error executing the tool. Message: Internal error. Code: -32603", toolExecutionResultString);
    }

    @Test
    public void executeToolThatThrowsBusinessError() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = toolProviderResult.findToolExecutorByName("error");
        String toolExecutionResultString = executor.execute(new ToolExecutionRequest("error"), null);
        assertEquals("There was an error executing the tool. Message: Internal error. Code: -32603", toolExecutionResultString);
    }

    @Test
    public void executeToolThatReturnsError() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = toolProviderResult.findToolExecutorByName("errorResponse");

        String toolExecutionResultString = executor.execute(new ToolExecutionRequest("errorResponse"), null);
        assertEquals("There was an error executing the tool. The tool returned: This is an actual error", toolExecutionResultString);
    }

    @Test
    public void timeout() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = toolProviderResult.findToolExecutorByName("longOperation");

        String toolExecutionResultString = executor.execute(new ToolExecutionRequest("longOperation"), null);
        assertEquals("There was a timeout executing the tool", toolExecutionResultString);
    }

    ToolProviderResult obtainTools() {
        McpToolProvider toolProvider = new McpToolProvider();
        toolProvider.setMcpClient(mcpClient);

        return toolProvider.provideTools(null);
    }
}
