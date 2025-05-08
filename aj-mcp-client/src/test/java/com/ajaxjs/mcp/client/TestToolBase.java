package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.protocol.tools.ToolItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class TestToolBase {
    static IMcpClient mcpClient;

    @Test
    public void resourceList() {
        List<ToolItem> tools = mcpClient.listTools();
        assertEquals(7, tools.size());
    }

    @Test
    public void executeTool() {
        String toolExecutionResultString = mcpClient.callTool("echoString", "{\"input\": \"hi\"}");
        assertEquals("hi", toolExecutionResultString);
    }

    McpToolProvider.McpToolProviderResult obtainTools() {
        McpToolProvider toolProvider = new McpToolProvider();
        toolProvider.setMcpClient(mcpClient);

        return toolProvider.provideTools();
    }
}
