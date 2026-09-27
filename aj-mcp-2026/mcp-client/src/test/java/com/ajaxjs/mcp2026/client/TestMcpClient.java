package com.ajaxjs.mcp2026.client;

import com.ajaxjs.mcp2026.client.model.Subscription;
import com.ajaxjs.mcp2026.core.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies client correlation, deadlines, subscriptions, and transport lifecycle behavior.
 */
class TestMcpClient {
    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    static final class Fake implements ClientTransport {
        Listener listener;
        Consumer<RpcRequest> send = r -> {
        };
        List<RequestId> cancellations = new CopyOnWriteArrayList<>();

        public void start(Listener listener) {
            this.listener = listener;
        }

        public void send(RpcRequest request) {
            send.accept(request);
        }

        public void cancel(RequestId id) {
            cancellations.add(id);
        }

        public void close() {
        }
    }

    @Test
    void multiplexesOutOfOrderAndSeparatesRemoteErrors() throws Exception {
        Fake transport = new Fake();
        try (McpClient client = new McpClient(transport)) {
            RpcRequest a = client.request("tools/call", map("name", "a")), b = client.request("tools/call", map("name", "b"));
            CompletableFuture<RpcSuccess> fa = client.exchange(a, 1000), fb = client.exchange(b, 1000);
            transport.listener.message(null, new RpcSuccess(b.getId(), ToolResults.text("b")));
            transport.listener.message(null, new RpcError(a.getId(), -32602, "invalid"));
            assertEquals(b.getId(), fb.get(1, TimeUnit.SECONDS).getId());
            assertTrue(assertThrows(ExecutionException.class, () -> fa.get(1, TimeUnit.SECONDS)).getCause() instanceof McpRemoteException);
            assertEquals(0, client.pendingCount());
        }
    }

    @Test
    void timeoutsCancellationAndCloseReleasePending() throws Exception {
        Fake transport = new Fake();
        McpClient client = new McpClient(transport);
        CompletableFuture<McpResult> timed = client.callAsync("tools/list", map(), 30);
        assertTrue(assertThrows(ExecutionException.class, () -> timed.get(2, TimeUnit.SECONDS)).getCause() instanceof TimeoutException);
        CompletableFuture<McpResult> cancelled = client.callAsync("tools/list", map(), 1000);
        assertTrue(cancelled.cancel(true));
        assertEquals(0, client.pendingCount());
        CompletableFuture<McpResult> closed = client.callAsync("tools/list", map(), 1000);
        client.close();
        client.close();
        assertThrows(ExecutionException.class, () -> closed.get(1, TimeUnit.SECONDS));
        assertEquals(0, client.pendingCount());
        assertEquals(3, transport.cancellations.size());
        try (McpClient another = new McpClient(new Fake())) {
            assertThrows(IllegalArgumentException.class, () -> another.callAsync("tools/list", map(), -1));
        }
    }

    @Test
    void wrongScopedResponseAndTransportFailureDoNotHang() {
        Fake transport = new Fake();
        try (McpClient client = new McpClient(transport)) {
            RpcRequest a = client.request("tools/list", map());
            CompletableFuture<RpcSuccess> future = client.exchange(a, 1000);
            transport.listener.message(a.getId(), new RpcSuccess(RequestId.of("wrong"), McpResult.empty()));
            assertThrows(CompletionException.class, future::join);
            CompletableFuture<McpResult> other = client.callAsync("tools/list", map(), 1000);
            transport.listener.failed(null, new java.io.EOFException());
            assertThrows(CompletionException.class, other::join);
        }
    }

    @Test
    void drivesMrtrWithNewIdAndUnchangedOpaqueState() throws Exception {
        Fake transport = new Fake();
        List<RpcRequest> requests = new CopyOnWriteArrayList<>();
        transport.send = r -> {
            requests.add(r);
            McpResult result = requests.size() == 1 ? McpResult.inputRequired(map("roots", map("method", "roots/list")), "opaque")
                    : ToolResults.text("done");
            transport.listener.message(r.getId(), new RpcSuccess(r.getId(), result));
        };
        try (McpClient client = new McpClient(transport, RequestMetadata.create(map("roots", map())), n -> {
        })) {
            McpResult result = client.callWithInputs("tools/call", map("name", "x"), 2000, 3,
                    input -> map("roots", map("roots", Collections.emptyList()))).get(3, TimeUnit.SECONDS);
            assertEquals("complete", result.getResultType());
            assertEquals(2, requests.size());
            assertNotEquals(requests.get(0).getId(), requests.get(1).getId());
            assertEquals("opaque", requests.get(1).getParams().get("requestState"));
        }
    }

    @Test
    void boundsInputHandlerAndRoundCount() throws Exception {
        Fake transport = new Fake();
        transport.send = r -> transport.listener.message(r.getId(), new RpcSuccess(r.getId(), McpResult.inputRequired(null, "again")));
        try (McpClient client = new McpClient(transport)) {
            CompletableFuture<McpResult> exhausted = client.callWithInputs("tools/call", map("name", "x"), 1000, 2, input -> map());
            assertThrows(ExecutionException.class, () -> exhausted.get(2, TimeUnit.SECONDS));
            CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
            CompletableFuture<McpResult> blocked = client.callWithInputs("tools/call", map("name", "x"), 100, 3, input -> {
                entered.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ex) {
                    interrupted.countDown();
                    throw ex;
                }
                return map();
            });
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> blocked.get(2, TimeUnit.SECONDS));
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void closeFinishesMrtrWhileInputHandlerIsWaiting() throws Exception {
        Fake transport = new Fake();
        transport.send = r -> transport.listener.message(r.getId(), new RpcSuccess(r.getId(), McpResult.inputRequired(null, "again")));
        CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        try (McpClient client = new McpClient(transport)) {
            CompletableFuture<McpResult> result = client.callWithInputs("tools/call", map("name", "x"), 5000, 3, input -> {
                entered.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ex) {
                    interrupted.countDown();
                    throw ex;
                }
                return map();
            });
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            client.close();
            assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void subscriptionRequiresAckAndAcceptedFilter() throws Exception {
        Fake transport = new Fake();
        List<RpcRequest> sent = new ArrayList<>();
        transport.send = sent::add;
        try (McpClient client = new McpClient(transport)) {
            List<RpcNotification> updates = new ArrayList<>();
            Subscription sub = client.subscribe(map("toolsListChanged", true), 2000, updates::add);
            RpcRequest listen = sent.get(0);
            transport.listener.message(null, Subscriptions.acknowledge(listen, map("toolsListChanged", true)));
            assertNotNull(sub.acknowledged().get(1, TimeUnit.SECONDS));
            transport.listener.message(null, new RpcNotification("notifications/tools/list_changed",
                    map("_meta", map(Subscriptions.SUBSCRIPTION_ID, listen.getId().getValue()))));
            assertEquals(1, updates.size());
            transport.listener.message(null, Subscriptions.closed(listen));
            sub.completion().get(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void paginationRejectsRepeatedCursorsAndHttpConfigRejectsRedirectCredentialPaths() {
        Fake transport = new Fake();
        transport.send = r -> transport.listener.message(r.getId(), new RpcSuccess(r.getId(),
                McpResult.complete(map("tools", Collections.emptyList(), "ttlMs", 0, "cacheScope", "private", "nextCursor", "same"))));
        try (McpClient client = new McpClient(transport)) {
            assertThrows(IllegalStateException.class, client::listTools);
        }
        assertThrows(IllegalArgumentException.class, () -> new HttpClientTransport("file:///tmp"));
        assertThrows(IllegalArgumentException.class, () -> new HttpClientTransport("http://user:secret@localhost/mcp"));
        assertThrows(IllegalArgumentException.class, () -> new HttpClientTransport("http://localhost/mcp", Collections.singletonMap("Mcp-Method", "bad"), 1000, 1));
    }
}
