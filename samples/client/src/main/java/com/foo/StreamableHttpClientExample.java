package com.foo;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.client.transport.StreamableHttpTransport;
import com.ajaxjs.mcp.protocol.client.Root;
import com.ajaxjs.mcp.protocol.ProtocolVersion;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.utils.RequestMeta;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Run after com.foo.streamable.DemoApplication; no external services are required. */
public class StreamableHttpClientExample {
    public static void main(String[] args) throws Exception {
        String endpoint = args.length == 0 ? "http://127.0.0.1:8081/mcp" : args[0];
        StreamableHttpTransport transport = StreamableHttpTransport.builder()
                .endpointUrl(endpoint).openEventStream(true).timeout(Duration.ofSeconds(10)).build();

        try (McpClient client = McpClient.builder().transport(transport)
                .protocolVersion(ProtocolVersion.LATEST.value()).requestTimeout(Duration.ofSeconds(10)).build()) {
            CountDownLatch progress = new CountDownLatch(3);
            client.onNotification("notifications/progress", message -> {
                System.out.println("Progress: " + message);
                progress.countDown();
            });
            // Built-in roots/list handling answers reverse requests; no files are read or uploaded.
            client.setRoots(Collections.singletonList(new Root(
                    Paths.get(".").toAbsolutePath().normalize().toUri().toString(), "Sample workspace")), false);
            client.initialize();
            // Initialization alone does not guarantee the optional GET is ready for reverse calls.
            transport.getEventStreamReady().get(10, TimeUnit.SECONDS);
            System.out.println("Protocol: " + client.getNegotiatedProtocolVersion());
            System.out.println("Tools: " + client.listTools());
            System.out.println("Time: " + client.callTool(new CallToolRequest("serverTime")));
            System.out.println(client.callTool(new CallToolRequest("greet")));
            System.out.println(client.callTool("greet", "{\"name\":\"AJ-MCP\"}"));
            CallToolRequest work = new CallToolRequest("progressDemo");
            work.getParams().setMeta(new RequestMeta("demo-progress"));
            System.out.println(client.callTool(work));

            if (!progress.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException("Missing progress notifications");

            System.out.println("Client roots: " + client.callTool(new CallToolRequest("clientRoots")));
            System.out.println("Resources: " + client.listResources());
            System.out.println(client.readResource("demo://welcome"));
            System.out.println("Prompts: " + client.listPrompts());
            System.out.println(client.getPrompt("explain", "{\"topic\":\"MCP Streamable HTTP\"}"));
        } // Sends DELETE and closes GET, pending requests, and client resources.
    }
}
