package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.client.HttpClientTransport;
import com.ajaxjs.mcp2026.client.McpClient;
import com.ajaxjs.mcp2026.core.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ajaxjs.mcp2026.server.TestServerRuntime.map;
import static com.ajaxjs.mcp2026.server.TestServerRuntime.tool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies server behavior against a real request-scoped HTTP/SSE peer.
 */
@Timeout(10)
class TestHttpPeer {
    /**
     * Strict codec used by the in-process HTTP peer to construct protocol responses.
     */
    private final McpCodec codec = new McpCodec();

    @Test
    void clientAcceptsJsonAndSseCommentsCrLfAndMultilineData() throws Exception {
        AtomicInteger count = new AtomicInteger();
        HttpServer peer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        peer.createContext("/mcp", e -> {
            RpcRequest request = (RpcRequest) codec.decodeUtf8(WireIO.readAll(e.getRequestBody()));
            RpcSuccess response = new RpcSuccess(request.getId(), Discovery.result(map(), "peer", "1"));
            if (count.getAndIncrement() == 0) reply(e, 200, "application/json", codec.encode(response));
            else {
                String json = codec.encode(response);
                reply(e, 200, "text/event-stream", ": comment\r\nid: ignored\r\ndata: " + json.substring(0, 1) + "\r\ndata: " + json.substring(1) + "\r\n\r\n");
            }
        });
        peer.start();
        try (McpClient client = new McpClient(new HttpClientTransport(url(peer)))) {
            client.discover();
            client.discover();
        } finally {
            peer.stop(0);
        }
    }

    @Test
    void brokenStreamsAndWrongIdsFailWithoutWaitingForDeadline() throws Exception {
        for (String mode : Arrays.asList("eof", "id", "type")) {
            HttpServer peer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            peer.createContext("/mcp", e -> {
                WireIO.readAll(e.getRequestBody());
                if (mode.equals("eof")) reply(e, 200, "text/event-stream", ": no final result\n\n");
                else if (mode.equals("id"))
                    reply(e, 200, "application/json", codec.encode(new RpcSuccess(RequestId.of("other"), Discovery.result(map(), "peer", "1"))));
                else reply(e, 200, "text/plain", "not MCP");
            });
            peer.start();
            try (McpClient client = new McpClient(new HttpClientTransport(url(peer)))) {
                assertThrows(ExecutionException.class, () -> client.callAsync(Discovery.METHOD, map(), 3000).get(2, TimeUnit.SECONDS));
                assertEquals(0, client.pendingCount());
            } finally {
                peer.stop(0);
            }
        }
    }

    @Test
    void redirectNeverForwardsAuthorizationOrReplaysRequest() throws Exception {
        AtomicInteger redirected = new AtomicInteger();
        HttpServer peer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        peer.createContext("/mcp", e -> {
            WireIO.readAll(e.getRequestBody());
            e.getResponseHeaders().set("Location", "/other");
            e.sendResponseHeaders(307, -1);
            e.close();
        });
        peer.createContext("/other", e -> {
            redirected.incrementAndGet();
            reply(e, 200, "text/plain", "bad");
        });
        peer.start();
        try (McpClient client = new McpClient(new HttpClientTransport(url(peer), Collections.singletonMap("Authorization", "Bearer test"), 1000, 2))) {
            assertThrows(CompletionException.class, client::discover);
            assertEquals(0, redirected.get());
        } finally {
            peer.stop(0);
        }
    }

    @Test
    void invalidMirroredToolDefinitionsAreExcludedEvenOnGenericListApi() throws Exception {
        HttpServer peer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        peer.createContext("/mcp", e -> {
            RpcRequest r = (RpcRequest) codec.decodeUtf8(WireIO.readAll(e.getRequestBody()));
            ToolDefinition invalid = ToolDefinition.create("bad", map("type", "object", "properties", map("x", map("type", "number", "x-mcp-header", "X"))));
            McpResult result = McpResult.complete(map("tools", Arrays.asList(tool("good").toMap(), invalid.toMap()), "ttlMs", 0, "cacheScope", "private"));
            reply(e, 200, "application/json", codec.encode(new RpcSuccess(r.getId(), result)));
        });
        peer.start();
        try (McpClient client = new McpClient(new HttpClientTransport(url(peer)))) {
            assertEquals(1, ((List<?>) client.call("tools/list", map(), 1000).toMap().get("tools")).size());
            assertEquals("good", client.listTools().get(0).getName());
        } finally {
            peer.stop(0);
        }
    }

    @Test
    void authenticationSuppliesPrincipalAndRejectsBeforeBusinessDispatch() throws Exception {
        try (McpServer server = new McpServer("test", "1")) {
            server.tool(tool("who"), ctx -> ToolResults.text(ctx.principal().getName()));
            try (HttpServerTransport http = new HttpServerTransport(server, new InetSocketAddress("127.0.0.1", 0), "/mcp", Collections.emptySet(), e -> {
                if (!"Bearer test".equals(e.getRequestHeaders().getFirst("Authorization")))
                    throw new SecurityException();
                return () -> "alice";
            }, 4)) {
                http.start();
                try (McpClient denied = new McpClient(new HttpClientTransport(http.endpoint().toString()))) {
                    assertThrows(CompletionException.class, denied::discover);
                }
                try (McpClient allowed = new McpClient(new HttpClientTransport(http.endpoint().toString(), Collections.singletonMap("Authorization", "Bearer test"), 2000, 2))) {
                    assertEquals("alice", TestTransports.text(allowed.callTool("who", map())));
                }
            }
        }
    }

    static String url(HttpServer peer) {
        return "http://127.0.0.1:" + peer.getAddress().getPort() + "/mcp";
    }

    static void reply(HttpExchange e, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        e.getResponseHeaders().set("Content-Type", type);
        e.sendResponseHeaders(status, bytes.length);
        e.getResponseBody().write(bytes);
        e.close();
    }
}
