package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.client.HttpClientTransport;
import com.ajaxjs.mcp2026.client.McpClient;
import com.ajaxjs.mcp2026.client.StdioClientTransport;
import com.ajaxjs.mcp2026.client.model.Subscription;
import com.ajaxjs.mcp2026.core.*;
import com.ajaxjs.mcp2026.server.example.EchoServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import static com.ajaxjs.mcp2026.server.TestServerRuntime.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies STDIO and HTTP transport framing, closure, and failure paths.
 */
@Timeout(20)
class TestTransports {
    static ToolDefinition echo() {
        return ToolDefinition.create("echo", map("type", "object", "properties",
                map("text", map("type", "string", "x-mcp-header", "Text")), "required", Arrays.asList("text")));
    }

    static McpServer server() {
        return new McpServer("integration", "1").tool(echo(), ctx -> {
            ctx.progress(1d, 1d, "complete");
            return ToolResults.text((String) ((Map<?, ?>) ctx.request().getParams().get("arguments")).get("text"));
        });
    }

    static String text(McpResult result) {
        return (String) ((Map<?, ?>) ((List<?>) result.toMap().get("content")).get(0)).get("text");
    }

    @Test
    void realHttpDiscoveryUnicodeCustomHeadersAndFeatures() throws Exception {
        List<RpcNotification> progress = new CopyOnWriteArrayList<>();
        try (McpServer server = server(); HttpServerTransport http = new HttpServerTransport(server, 0)) {
            server.resource(map("uri", "test://config", "name", "config"), ctx -> McpResult.complete(map("contents",
                    Collections.singletonList(map("uri", "test://config", "text", "value")), "ttlMs", 0, "cacheScope", "private")));
            server.prompt(map("name", "greet"), ctx -> McpResult.complete(map("messages", Arrays.asList(
                    map("role", "user", "content", map("type", "text", "text", "hello"))))));
            server.method("completion/complete", ctx -> McpResult.complete(map("completion", map("values", Arrays.asList("alice")))));
            http.start();
            try (McpClient client = new McpClient(new HttpClientTransport(http.endpoint().toString()),
                    RequestMetadata.create(map()).with("progressToken", "token"), progress::add)) {
                assertTrue(client.discover().toMap().containsKey("supportedVersions"));
                assertEquals(1, client.listTools().size());
                assertEquals(" 世界\n ", text(client.callTool("echo", map("text", " 世界\n "))));
                assertEquals(1, progress.size());
                assertEquals(1, client.listAll("resources/list", "resources", 2).size());
                assertEquals(1, client.listAll("prompts/list", "prompts", 2).size());
                assertTrue(client.listAll("resources/templates/list", "resourceTemplates", 2).isEmpty());
                assertTrue(client.call("resources/read", map("uri", "test://config"), 1000).toMap().containsKey("contents"));
                assertTrue(client.call("prompts/get", map("name", "greet"), 1000).toMap().containsKey("messages"));
                assertTrue(client.call("completion/complete", map("ref", map("type", "ref/prompt", "name", "greet"),
                        "argument", map("name", "x", "value", "a")), 1000).toMap().containsKey("completion"));
            }
        }
    }

    @Test
    void httpSubscriptionIsIndependentOfOtherPostsAndCancel() throws Exception {
        try (McpServer server = server(); HttpServerTransport http = new HttpServerTransport(server, 0)) {
            http.start();
            try (McpClient client = new McpClient(new HttpClientTransport(http.endpoint().toString()))) {
                CountDownLatch update = new CountDownLatch(1);
                Subscription sub = client.subscribe(map("toolsListChanged", true), 5000, n -> update.countDown());
                sub.acknowledged().get(2, TimeUnit.SECONDS);
                server.tool(tool("second"), ctx -> ToolResults.text("ok"));
                assertTrue(update.await(2, TimeUnit.SECONDS));
                assertEquals(2, client.listTools().size());
                sub.close();
                assertEquals("still works", text(client.callTool("echo", map("text", "still works"))));
                assertEquals(0, client.pendingCount());
            }
        }
    }

    @Test
    void httpHeaderOriginMethodAndNotificationBoundaries() throws Exception {
        try (McpServer server = server(); HttpServerTransport http = new HttpServerTransport(server, 0)) {
            http.start();
            String endpoint = http.endpoint().toString();
            RpcRequest request = request(1, "tools/call", map("name", "echo", "arguments", map("text", "hello")));
            Map<String, String> headers = new LinkedHashMap<>(HttpMetadata.headers(request, echo()));
            headers.put("Mcp-Name", "wrong");
            assertEquals(400, raw(endpoint, "POST", new McpCodec().encode(request), headers).status);
            // HttpURLConnection suppresses Origin unless a JVM-wide unsafe-header flag is set.
            // Use a raw socket so this verifies what actually arrives at the server.
            try (Socket socket = new Socket("127.0.0.1", http.endpoint().getPort())) {
                socket.setSoTimeout(2000);
                socket.getOutputStream().write(("POST /mcp HTTP/1.1\r\nHost: localhost\r\nOrigin: https://evil.example\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
                assertTrue(WireIO.line(socket.getInputStream()).contains("403"));
            }
            assertEquals(405, raw(endpoint, "GET", null, mapStrings()).status);
            assertEquals(404, raw(endpoint + "/extra", "POST", "{}", mapStrings()).status);
            Response accepted = raw(endpoint, "POST", new McpCodec().encode(new RpcNotification("notifications/cancelled", map("requestId", 1))), mapStrings());
            assertEquals(202, accepted.status);
            assertEquals("", accepted.body);
            assertEquals(400, raw(endpoint, "POST", "[{}]", mapStrings()).status);
        }
    }

    @Test
    void httpTimeoutInterruptsServerHandlerAndDoesNotReplay() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();
        try (McpServer server = server(); HttpServerTransport http = new HttpServerTransport(server, 0)) {
            server.tool(tool("blocked"), ctx -> {
                count.incrementAndGet();
                entered.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ex) {
                    interrupted.countDown();
                    throw ex;
                }
                return ToolResults.text("late");
            });
            http.start();
            try (McpClient client = new McpClient(new HttpClientTransport(http.endpoint().toString()))) {
                client.listTools();
                CompletableFuture<McpResult> future = client.callAsync("tools/call", map("name", "blocked"), 250);
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                assertThrows(ExecutionException.class, () -> future.get(3, TimeUnit.SECONDS));
                assertTrue(interrupted.await(5, TimeUnit.SECONDS));
                assertEquals(1, count.get());
            }
        }
    }

    @Test
    void httpMrtrReusesProtectedStateWithExplicitInputHandler() throws Exception {
        RequestState state = new RequestState(RequestState.newKey(), "test");
        try (McpServer server = server(); HttpServerTransport http = new HttpServerTransport(server, 0)) {
            server.tool(tool("roots"), ctx -> {
                if (!ctx.request().getParams().containsKey("requestState")) return McpResult.inputRequired(
                        map("r", map("method", "roots/list")), state.seal(ctx.request(), ctx.principal(), map("stage", 1), 5000));
                state.open(ctx.request(), ctx.principal());
                Map<?, ?> inputs = (Map<?, ?>) ctx.request().getParams().get("inputResponses");
                server.getProtocol().requireType("ListRootsResult", inputs.get("r"));
                return ToolResults.text("resumed");
            });
            http.start();
            try (McpClient client = new McpClient(new HttpClientTransport(http.endpoint().toString()), RequestMetadata.create(map("roots", map())), n -> {
            })) {
                client.listTools();
                assertEquals("resumed", text(client.callWithInputs("tools/call", map("name", "roots"), 3000, 3,
                        input -> map("r", map("roots", Collections.emptyList()))).get(4, TimeUnit.SECONDS)));
            }
        }
    }

    @Test
    void stdioActualChildProcessAndUtf8() throws Exception {
        String java = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
        try (McpClient client = new McpClient(StdioClientTransport.launch(Arrays.asList(java, "-cp",
                System.getProperty("java.class.path"), EchoServer.class.getName())))) {
            assertEquals(1, client.listTools().size());
            assertEquals("你好", text(client.callTool("echo", map("text", "你好"))));
            assertEquals(0, client.pendingCount());
        }
    }

    @Test
    void stdioInMemoryNotificationSuppressionAndParseErrorRecovery() throws Exception {
        RpcRequest discover = request(2, Discovery.METHOD, map());
        String input = "{bad}\n{\"jsonrpc\":\"2.0\",\"method\":\"unknown\"}\n" + new McpCodec().encode(discover) + "\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (McpServer server = server(); StdioServerTransport stdio = new StdioServerTransport(server,
                new ByteArrayInputStream(input.getBytes("UTF-8")), output)) {
            stdio.run();
            // EOF cancels unfinished requests; only the parse error is guaranteed to have been written.
            String all = output.toString("UTF-8");
            assertTrue(all.contains("-32700"));
            assertFalse(all.contains("Unknown core method"));
        }
    }

    static Map<String, String> mapStrings() {
        return new LinkedHashMap<>();
    }

    static Response raw(String endpoint, String method, String body, Map<String, String> headers) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(2000);
        c.setReadTimeout(2000);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json, text/event-stream");
        headers.forEach(c::setRequestProperty);
        try {
            if (body != null) {
                c.setDoOutput(true);
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.getBytes("UTF-8"));
                }
            }
            int status = c.getResponseCode();
            InputStream stream = status >= 400 ? c.getErrorStream() : c.getInputStream();
            String text = "";
            if (stream != null) try (InputStream in = stream) {
                text = new String(WireIO.readAll(in), "UTF-8");
            }
            return new Response(status, text);
        } finally {
            c.disconnect();
        }
    }

    /**
     * Immutable raw HTTP response captured by transport boundary tests.
     */
    static final class Response {
        /**
         * HTTP status returned by the peer.
         */
        final int status;
        /**
         * Fully read response body, possibly empty.
         */
        final String body;

        /**
         * Creates a captured response for one raw HTTP exchange.
         *
         * @param status HTTP status code
         * @param body   response text decoded by the test helper
         */
        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}
