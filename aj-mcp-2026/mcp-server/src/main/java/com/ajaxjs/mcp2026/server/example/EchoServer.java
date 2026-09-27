package com.ajaxjs.mcp2026.server.example;

import com.ajaxjs.mcp2026.core.ToolDefinition;
import com.ajaxjs.mcp2026.core.ToolResults;
import com.ajaxjs.mcp2026.server.McpServer;
import com.ajaxjs.mcp2026.server.StdioServerTransport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executable STDIO example: protocol only on stdout. Launch with the module and dependency JARs on classpath.
 */
public final class EchoServer {
    /**
     * Prevents instantiation of this executable example holder.
     */
    private EchoServer() {
    }

    /**
     * Launches the sample STDIO echo server. Standard output is reserved for protocol messages.
     *
     * @param args ignored command-line arguments
     * @throws Exception if server startup or stream processing fails
     */
    public static void main(String[] args) throws Exception {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections.singletonMap("text", Collections.singletonMap("type", "string")));
        schema.put("required", Collections.singletonList("text"));

        try (McpServer server = new McpServer("aj-mcp-next-echo", "1");
             StdioServerTransport stdio = new StdioServerTransport(server, System.in, System.out)) {
            server.tool(ToolDefinition.create("echo", schema), context -> {
                Map<?, ?> arguments = (Map<?, ?>) context.request().getParams().get("arguments");
                return ToolResults.text((String) arguments.get("text"));
            });
            stdio.run();
        }
    }
}
