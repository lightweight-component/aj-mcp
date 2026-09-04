package com.foo;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.client.transport.HttpMcpTransport;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.client.transport.StreamableHttpTransport;

import java.util.Arrays;

/**
 * Demonstrates connecting an AJ-MCP client to STDIO, legacy SSE, or Streamable HTTP servers.
 */
public class MyApp {
    /**
     * Connects to a server selected by command-line arguments and prints its advertised features.
     *
     * @param args transport name followed by its endpoint or command.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        try (McpClient client = connect(args)) {
            System.out.println("Negotiated MCP version: " + client.getNegotiatedProtocolVersion());
            System.out.println("Tools: " + client.listTools());
            System.out.println("Prompts: " + client.listPrompts());
            System.out.println("Resources: " + client.listResources());
            System.out.println("Resource templates: " + client.listResourceTemplates());
        }
    }

    /**
     * Creates and initializes a client for the requested transport.
     *
     * @param args transport name followed by its endpoint or command.
     * @return an initialized MCP client.
     */
    private static McpClient connect(String[] args) {
        if ("stdio".equals(args[0]))
            return McpClient.createStdioMcpClient(Arrays.copyOfRange(args, 1, args.length));

        McpTransport transport;
        if ("sse".equals(args[0]))
            transport = new HttpMcpTransport(args[1]);
        else if ("http".equals(args[0]))
            transport = new StreamableHttpTransport(args[1]);
        else
            throw new IllegalArgumentException("Unsupported transport: " + args[0]);

        McpClient client = McpClient.builder().transport(transport).build();
        client.initialize();
        return client;
    }

    /**
     * Prints supported command-line forms.
     */
    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  stdio <command> [arguments...]");
        System.err.println("  sse <http://host:port/sse>");
        System.err.println("  http <http://host:port/mcp>");
    }
}
