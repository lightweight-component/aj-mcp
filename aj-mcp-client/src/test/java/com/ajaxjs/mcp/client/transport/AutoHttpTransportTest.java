package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class AutoHttpTransportTest {
    private HttpServer http;
    private ExecutorService workers;
    private AutoHttpTransport transport;
    private final CountDownLatch stop = new CountDownLatch(1);

    private void start(HttpHandler handler) throws IOException {
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        workers = Executors.newCachedThreadPool(); http.setExecutor(workers);
        http.createContext("/", handler); http.start();
        transport = new AutoHttpTransport("http://127.0.0.1:" + http.getAddress().getPort() + "/entry",
                false, Duration.ofSeconds(2), Collections.singletonMap("Authorization", "Bearer test"));
    }

    @AfterEach void close() {
        if (transport != null) transport.close();
        stop.countDown();
        if (http != null) http.stop(0);
        if (workers != null) workers.shutdownNow();
    }

    private static JsonNode body(HttpExchange e) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[1024]; int n;
        while ((n = e.getRequestBody().read(b)) != -1) out.write(b, 0, n);
        return JsonUtils.json2Node(new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    private static void reply(HttpExchange e, int code, String text) throws IOException {
        if (text == null) { e.sendResponseHeaders(code, -1); e.close(); return; }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json");
        e.sendResponseHeaders(code, bytes.length); e.getResponseBody().write(bytes); e.close();
    }

    private static String response(JsonNode request, String version) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + request.get("id") + ",\"result\":"
                + ("initialize".equals(request.path("method").asText())
                ? "{\"protocolVersion\":\"" + version + "\",\"capabilities\":{},\"serverInfo\":{\"name\":\"test\",\"version\":\"1\"}}" : "{}") + "}";
    }

    @Test void modernServerDoesNotProbeLegacy() throws Exception {
        AtomicInteger gets = new AtomicInteger();
        start(e -> {
            if ("GET".equals(e.getRequestMethod())) { gets.incrementAndGet(); reply(e, 405, null); return; }
            JsonNode request = body(e);
            reply(e, request.has("id") ? 200 : 202, request.has("id") ? response(request, "2025-06-18") : null);
        });
        McpClient.builder().transport(transport).build().initialize();
        assertFalse(transport.isLegacySse()); assertEquals(0, gets.get());
        PingRequest ping = new PingRequest(); ping.setId(2L);
        assertTrue(transport.sendRequestWithResponse(ping).get(2, TimeUnit.SECONDS).has("result"));
    }

    @ParameterizedTest @ValueSource(ints = {400, 404, 405, 415})
    void fallsBackAndPreservesHeadersAndHandlers(int rejection) throws Exception {
        String selectedVersion = rejection == 415 ? "2025-06-18" : "2024-11-05";
        AtomicReference<OutputStream> stream = new AtomicReference<>();
        AtomicBoolean headersPresent = new AtomicBoolean(true);
        CompletableFuture<JsonNode> notice = new CompletableFuture<>();
        start(e -> {
            if (!"Bearer test".equals(e.getRequestHeaders().getFirst("Authorization"))) headersPresent.set(false);
            if ("GET".equals(e.getRequestMethod())) {
                e.getResponseHeaders().set("Content-Type", "text/event-stream"); e.sendResponseHeaders(200, 0);
                stream.set(e.getResponseBody());
                stream.get().write("event: endpoint\ndata: /messages?session=one\n\n".getBytes(StandardCharsets.UTF_8));
                stream.get().flush();
                try { stop.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                e.close(); return;
            }
            if ("/entry".equals(e.getRequestURI().getPath())) { reply(e, rejection, null); return; }
            JsonNode request = body(e);
            if (!"initialize".equals(request.path("method").asText())
                    && !selectedVersion.equals(e.getRequestHeaders().getFirst("MCP-Protocol-Version")))
                headersPresent.set(false);
            if (request.has("id")) {
                synchronized (stream) {
                    stream.get().write(("event: message\ndata: " + response(request, selectedVersion) + "\n\n")
                            .getBytes(StandardCharsets.UTF_8)); stream.get().flush();
                }
            }
            reply(e, 202, null);
        });
        McpClient client = McpClient.builder().transport(transport).build();
        client.onNotification("notifications/message", notice::complete); client.initialize();
        assertTrue(transport.isLegacySse()); assertEquals(selectedVersion, client.getNegotiatedProtocolVersion());
        PingRequest ping = new PingRequest(); ping.setId(9L);
        assertEquals(9, transport.sendRequestWithResponse(ping).get(2, TimeUnit.SECONDS).path("id").asInt());
        synchronized (stream) {
            stream.get().write("event: message\ndata: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/message\",\"params\":{}}\n\n".getBytes(StandardCharsets.UTF_8));
            stream.get().flush();
        }
        assertNotNull(notice.get(2, TimeUnit.SECONDS)); assertTrue(headersPresent.get());
        transport.close(); transport.close();
    }

    @ParameterizedTest @ValueSource(ints = {401, 403, 429, 500})
    void doesNotDowngradeAuthenticationOrServerFailures(int status) throws Exception {
        AtomicInteger gets = new AtomicInteger();
        start(e -> { if ("GET".equals(e.getRequestMethod())) gets.incrementAndGet(); reply(e, status, null); });
        assertThrows(RuntimeException.class, () -> McpClient.builder().transport(transport).build().initialize());
        assertEquals(0, gets.get()); assertFalse(transport.isLegacySse());
    }

    @Test void jsonRpcErrorDoesNotTriggerFallback() throws Exception {
        AtomicInteger gets = new AtomicInteger();
        start(e -> {
            if ("GET".equals(e.getRequestMethod())) gets.incrementAndGet();
            reply(e, 200, "{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32602,\"message\":\"Invalid params\"}}");
        });
        assertThrows(RuntimeException.class, () -> McpClient.builder().transport(transport).build().initialize());
        assertEquals(0, gets.get());
    }

    @Test void closeInterruptsLegacyEndpointDiscovery() throws Exception {
        CountDownLatch opened = new CountDownLatch(1);
        start(e -> {
            if (!"GET".equals(e.getRequestMethod())) { reply(e, 405, null); return; }
            e.getResponseHeaders().set("Content-Type", "text/event-stream"); e.sendResponseHeaders(200, 0);
            e.getResponseBody().write(": waiting\n\n".getBytes(StandardCharsets.UTF_8)); e.getResponseBody().flush();
            opened.countDown();
            try { stop.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            e.close();
        });
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<?> task = caller.submit(() -> McpClient.builder().transport(transport).build().initialize());
            assertTrue(opened.await(2, TimeUnit.SECONDS));
            transport.close();
            assertThrows(ExecutionException.class, () -> task.get(1, TimeUnit.SECONDS));
        } finally { caller.shutdownNow(); }
    }

    @Test void crossOriginEndpointIsRejectedBeforePostingCredentials() throws Exception {
        AtomicInteger posts = new AtomicInteger();
        start(e -> {
            if (!"GET".equals(e.getRequestMethod())) { posts.incrementAndGet(); reply(e, 405, null); return; }
            e.getResponseHeaders().set("Content-Type", "text/event-stream"); e.sendResponseHeaders(200, 0);
            e.getResponseBody().write("event: endpoint\ndata: http://127.0.0.1:1/messages\n\n".getBytes(StandardCharsets.UTF_8));
            e.getResponseBody().flush();
            try { stop.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            e.close();
        });
        assertThrows(RuntimeException.class, () -> McpClient.builder().transport(transport).build().initialize());
        assertEquals(1, posts.get());
    }
}
