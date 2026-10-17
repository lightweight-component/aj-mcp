package com.ajaxjs.mcp2026.sample.client;

import com.ajaxjs.mcp2026.client.HttpClientTransport;
import com.ajaxjs.mcp2026.client.McpClient;
import com.ajaxjs.mcp2026.core.McpResult;
import com.ajaxjs.mcp2026.core.ToolDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Standalone client demonstrating discovery, tool listing, and successful or failed business results. */
public final class SampleClient {
    /** Prevents construction of this command-line entry point. */
    private SampleClient() {
    }

    /**
     * Calls the sample server and exits after closing all client resources.
     * @param args optional endpoint URI followed by an optional order ID
     */
    public static void main(String[] args) {
        String endpoint = args.length > 0 ? args[0] : "http://127.0.0.1:8080/mcp";
        String orderId = args.length > 1 ? args[1] : "A1001";
        try (McpClient client = new McpClient(new HttpClientTransport(endpoint))) {
            System.out.println("服务信息：" + client.discover().toMap());
            for (ToolDefinition tool : client.listTools())
                System.out.println("发现工具：" + tool.getName());
            printResult(client.callTool("echo", Collections.singletonMap("text", "你好，MCP！")));
            printResult(client.callTool("query_order", Collections.singletonMap("orderId", orderId)));
        }
    }

    /**
     * Prints text content returned by the sample tools, including business failures.
     * @param result validated tool result containing MCP content items
     */
    private static void printResult(McpResult result) {
        boolean error = Boolean.TRUE.equals(result.toMap().get("isError"));
        for (Object item : (List<?>) result.toMap().get("content")) {
            Map<?, ?> content = (Map<?, ?>) item;
            if ("text".equals(content.get("type")))
                System.out.println((error ? "查询未完成：" : "工具返回：") + content.get("text"));
        }
    }
}
