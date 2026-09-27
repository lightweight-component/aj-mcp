package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.core.*;
import com.ajaxjs.mcp2026.server.model.Connection;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies server dispatch, registration, cancellation, and lifecycle isolation.
 */
class TestServerRuntime {
    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> value = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) value.put((String) pairs[i], pairs[i + 1]);
        return value;
    }

    static RpcRequest request(long id, String method, Map<String, Object> params) {
        return RpcRequest.create(RequestId.of(id), method, params, RequestMetadata.create(map()));
    }

    static ToolDefinition tool(String name) {
        return ToolDefinition.create(name, map("type", "object"));
    }

    @Test
    void notificationsNeverRespondIncludingInvalidAndUnknown() {
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1"); Connection c = server.connect(sent::add, null, false)) {
            c.receive(new RpcNotification("unknown", map()));
            c.receive(new RpcNotification("notifications/cancelled", map("requestId", 999)));
            c.receive(new RpcNotification("notifications/cancelled", map("requestId", true)));
            assertTrue(sent.isEmpty());
        }
    }

    @Test
    void validatesInputsOutputsAndPreservesLocalExceptionCause() throws Exception {
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        AtomicInteger invoked = new AtomicInteger();
        IllegalStateException business = new IllegalStateException("private secret");
        try (McpServer server = new McpServer("test", "1", 2, 2000, 2000, errors::add);
             Connection c = server.connect(sent::add, null, false)) {
            server.tool(ToolDefinition.create("checked", map("type", "object", "required", Arrays.asList("n"), "properties", map("n", map("type", "integer")))),
                    ctx -> {
                        invoked.incrementAndGet();
                        throw business;
                    });
            c.receive(request(1, "tools/call", map("name", "checked", "arguments", map("n", "bad")))).get(1, TimeUnit.SECONDS);
            assertEquals(0, invoked.get());
            assertEquals(-32602, ((RpcError) sent.get(0)).getCode());
            c.receive(request(2, "tools/call", map("name", "checked", "arguments", map("n", 1)))).get(1, TimeUnit.SECONDS);
            assertEquals(-32603, ((RpcError) sent.get(1)).getCode());
            assertFalse(((RpcError) sent.get(1)).getMessage().contains("secret"));
            assertTrue(errors.contains(business));
        }
    }

    @Test
    void connectionScopedIdsCancellationAndNoLateResponse() throws Exception {
        CountDownLatch entered = new CountDownLatch(2), interrupted = new CountDownLatch(1), release = new CountDownLatch(1);
        List<RpcMessage> left = new CopyOnWriteArrayList<>(), right = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1");
             Connection a = server.connect(left::add, null, false);
             Connection b = server.connect(right::add, null, false)) {
            server.tool(tool("block"), ctx -> {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException ex) {
                    interrupted.countDown();
                    throw ex;
                }
                return ToolResults.text("ok");
            });
            CompletableFuture<Void> fa = a.receive(request(1, "tools/call", map("name", "block")));
            CompletableFuture<Void> fb = b.receive(request(1, "tools/call", map("name", "block")));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            a.receive(new RpcNotification("notifications/cancelled", map("requestId", 1)));
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            fa.get(1, TimeUnit.SECONDS);
            assertEquals(1, b.activeCount());
            release.countDown();
            fb.get(1, TimeUnit.SECONDS);
            assertTrue(left.isEmpty());
            assertEquals(1, right.size());
        } finally {
            release.countDown();
        }
    }

    @Test
    void serverDeadlineAndCloseInterruptWork() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1", 1, 100, 1000, e -> {
        });
             Connection c = server.connect(sent::add, null, false)) {
            server.tool(tool("wait"), ctx -> {
                entered.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ex) {
                    interrupted.countDown();
                    throw ex;
                }
                return ToolResults.text("late");
            });
            CompletableFuture<Void> done = c.receive(request(1, "tools/call", map("name", "wait")));
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            done.get(2, TimeUnit.SECONDS);
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            assertEquals(1, sent.size());
            assertTrue(sent.get(0) instanceof RpcError);
        }
    }

    @Test
    void subscriptionAckFilteringAndGracefulLifetime() throws Exception {
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1", 1, 1000, 150, e -> {
        });
             Connection c = server.connect(sent::add, null, false)) {
            RpcRequest listen = Subscriptions.listen(RequestId.of(1), RequestMetadata.create(map()), map("toolsListChanged", true));
            CompletableFuture<Void> done = c.receive(listen);
            assertEquals(Subscriptions.ACKNOWLEDGED, ((RpcNotification) sent.get(0)).getMethod());
            server.resourceUpdated("test://x");
            assertEquals(1, sent.size());
            server.tool(tool("new"), ctx -> ToolResults.text("x"));
            assertEquals(2, sent.size());
            done.get(2, TimeUnit.SECONDS);
            assertTrue(sent.get(2) instanceof RpcSuccess);
            Subscriptions.validateClosed(listen, (RpcSuccess) sent.get(2));
            assertEquals(0, c.activeCount());
        }
    }

    @Test
    void duplicateInFlightIdsCloseOnlyAffectedConnection() throws Exception {
        CountDownLatch block = new CountDownLatch(1);
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1"); Connection c = server.connect(sent::add, null, false)) {
            server.tool(tool("wait"), ctx -> {
                block.await();
                return ToolResults.text("x");
            });
            RpcRequest req = request(1, "tools/call", map("name", "wait"));
            CompletableFuture<Void> done = c.receive(req);
            assertThrows(IllegalArgumentException.class, () -> c.receive(req));
            done.get(1, TimeUnit.SECONDS);
            assertEquals(0, c.activeCount());
            assertTrue(sent.isEmpty());
        } finally {
            block.countDown();
        }
    }

    @Test
    void snapshotsDescriptorsAndRejectsMissingPromptArguments() throws Exception {
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        Map<String, Object> descriptor = map("name", "p", "arguments", Arrays.asList(map("name", "q", "required", true)));
        try (McpServer server = new McpServer("test", "1"); Connection c = server.connect(sent::add, null, false)) {
            server.prompt(descriptor, ctx -> McpResult.complete(map("messages", Collections.emptyList())));
            descriptor.put("name", "mutated");
            c.receive(request(1, "prompts/get", map("name", "p"))).get(1, TimeUnit.SECONDS);
            assertEquals(-32602, ((RpcError) sent.get(0)).getCode());
            c.receive(request(2, "prompts/get", map("name", "p", "arguments", map("q", "hello")))).get(1, TimeUnit.SECONDS);
            assertTrue(sent.get(1) instanceof RpcSuccess);
        }
    }

    @Test
    void simultaneousWritersFailWithoutCrossContextDeadlock() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (McpServer server = new McpServer("test", "1");
             Connection c = server.connect(message -> {
                 throw new java.io.IOException("closed sink");
             }, null, false)) {
            server.tool(tool("parallel"), ctx -> {
                barrier.await(1, TimeUnit.SECONDS);
                return ToolResults.text("x");
            });
            CompletableFuture<Void> a = c.receive(request(1, "tools/call", map("name", "parallel")));
            CompletableFuture<Void> b = c.receive(request(2, "tools/call", map("name", "parallel")));
            CompletableFuture.allOf(a, b).get(2, TimeUnit.SECONDS);
            assertEquals(0, c.activeCount());
        }
    }

    @Test
    void inputRequiredWithoutClientCapabilityReturnsSpecificProtocolError() throws Exception {
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1"); Connection c = server.connect(sent::add, null, false)) {
            server.tool(tool("roots"), ctx -> McpResult.inputRequired(map("r", map("method", "roots/list")), null));
            c.receive(request(1, "tools/call", map("name", "roots"))).get(1, TimeUnit.SECONDS);
            assertEquals(ErrorCodes.MISSING_CLIENT_CAPABILITY, ((RpcError) sent.get(0)).getCode());
        }
    }

    @Test
    void serverShutdownCancelsStdioSubscriptionsWithoutClaimingUnknownFilters() {
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1"); Connection c = server.connect(sent::add, null, false)) {
            c.receive(Subscriptions.listen(RequestId.of(1), RequestMetadata.create(map()), map("toolsListChanged", true, "vendor/filter", true)));
            Map<?, ?> filter = (Map<?, ?>) ((RpcNotification) sent.get(0)).getParams().get("notifications");
            assertFalse(filter.containsKey("vendor/filter"));
            server.close();
            assertEquals("notifications/cancelled", ((RpcNotification) sent.get(1)).getMethod());
            assertEquals(0, c.activeCount());
        }
    }

    @Test
    void rejectsRegressingProgressBeforeEmission() throws Exception {
        List<RpcMessage> sent = new CopyOnWriteArrayList<>();
        try (McpServer server = new McpServer("test", "1"); Connection c = server.connect(sent::add, null, false)) {
            server.tool(tool("progress"), ctx -> {
                ctx.progress(2, 3d, null);
                ctx.progress(1, 3d, null);
                return ToolResults.text("bad");
            });
            RpcRequest request = RpcRequest.create(RequestId.of(1), "tools/call", map("name", "progress"), RequestMetadata.create(map()).with("progressToken", "p"));
            c.receive(request).get(1, TimeUnit.SECONDS);
            assertEquals(2, sent.size());
            assertTrue(sent.get(0) instanceof RpcNotification);
            assertEquals(ErrorCodes.INTERNAL_ERROR, ((RpcError) sent.get(1)).getCode());
        }
    }
}
