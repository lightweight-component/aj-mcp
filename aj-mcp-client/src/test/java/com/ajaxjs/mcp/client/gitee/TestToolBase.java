package com.ajaxjs.mcp.client.gitee;

import com.ajaxjs.mcp.client.IMcpClient;
import com.ajaxjs.mcp.client.McpToolProvider;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class TestToolBase {
    static IMcpClient mcpClient;

    @Test
    public void resourceList() {
        List<ToolItem> tools = mcpClient.listTools();
        System.out.println(tools);
    }

    @Test
    void verifyToolSpecifications() {

        ToolItem listUserRepos = obtainTools().findToolByName("list_user_repos");

        System.out.println(listUserRepos.getDescription());
        assertNotNull(listUserRepos, "Tool 'list_user_repos' should not be null");
    }


    @Test
    public void executeTool() {
        Function<CallToolRequest, String> executor = obtainTools().findToolExecutorByName("list_user_repos");
        String toolExecutionResultString = executor.apply(new CallToolRequest("list_user_repos"));
        System.out.println(toolExecutionResultString);
//        assertEquals("abc", toolExecutionResultString);
    }

    @Test
    public void executeNonExistentTool() {
        Function<CallToolRequest, String> executor = obtainTools().findToolExecutorByName("list_user_repos");

        String toolExecutionResultString = executor.apply(new CallToolRequest("THIS-TOOL-DOES-NOT-EXIST", "{\"input\": 1}"));
        assertEquals("There was an error executing the tool. " + "Message: Tool not found: THIS-TOOL-DOES-NOT-EXIST. Code: -32602", toolExecutionResultString);
    }

    @Test
    public void executeToolWithWrongArgumentType() {
        Function<CallToolRequest, String> executor = obtainTools().findToolExecutorByName("list_user_repos");

        String toolExecutionResultString = executor.apply(new CallToolRequest("list_user_repos", "{\"input\": 1}"));
        assertEquals("There was an error executing the tool. Message: Internal error. Code: -32603", toolExecutionResultString);
    }

    @Test
    public void executeToolThatThrowsBusinessError() {
        Function<CallToolRequest, String> executor = obtainTools().findToolExecutorByName("error");
        String toolExecutionResultString = executor.apply(new CallToolRequest("error"));
        assertEquals("There was an error executing the tool. Message: Internal error. Code: -32603", toolExecutionResultString);
    }

    @Test
    public void executeToolThatReturnsError() {
        Function<CallToolRequest, String> executor = obtainTools().findToolExecutorByName("errorResponse");

        String toolExecutionResultString = executor.apply(new CallToolRequest("errorResponse"));
        assertEquals("There was an error executing the tool. The tool returned: This is an actual error", toolExecutionResultString);
    }

    @Test
    public void timeout() {
        Function<CallToolRequest, String> executor = obtainTools().findToolExecutorByName("longOperation");

        String toolExecutionResultString = executor.apply(new CallToolRequest("longOperation"));
        assertEquals("There was a timeout executing the tool", toolExecutionResultString);
    }

    McpToolProvider.McpToolProviderResult obtainTools() {
        McpToolProvider toolProvider = new McpToolProvider();
        toolProvider.setMcpClient(mcpClient);

        return toolProvider.provideTools();
    }
}
