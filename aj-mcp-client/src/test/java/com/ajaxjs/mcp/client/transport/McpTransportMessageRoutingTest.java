package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Represents mcp transport message routing test.
 */
class McpTransportMessageRoutingTest {
    @Test
    void pingIsBuiltInAndPreservesIdsWithoutConsumingPendingRequests() {
        CapturingTransport transport = new CapturingTransport();
        Map<Long, CompletableFuture<JsonNode>> pending = new java.util.HashMap<>();
        pending.put(7L, new CompletableFuture<>());
        transport.start(pending);
        transport.setMessageHandlers(ignored -> {}, request -> {
            throw new AssertionError("Ping must bypass application handlers");
        });
        for (String id : new String[]{"7", "\"server-ping\""}) {
            transport.handle(JsonUtils.json2Node(
                    "{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":" + id + "}"));
            assertEquals(JsonUtils.json2Node(id), transport.sent.get().get("id"));
            assertEquals(JsonUtils.json2Node("{}"), transport.sent.get().get("result"));
        }
        assertEquals(false, pending.get(7L).isDone());
        transport.sent.set(null);
        transport.handle(JsonUtils.json2Node("{\"jsonrpc\":\"2.0\",\"method\":\"ping\"}"));
        assertEquals(null, transport.sent.get());
    }

    @Test
    void routesNotificationWithoutProducingResponse() {
        CapturingTransport transport = new CapturingTransport();
        AtomicReference<JsonNode> params = new AtomicReference<>();
        transport.setMessageHandlers(params::set, ignored -> null);

        transport.handle(JsonUtils.json2Node("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/progress\",\"params\":{\"progress\":1}}"));

        assertEquals(1, params.get().get("params").get("progress").asInt());
        assertEquals(null, transport.sent.get());
    }

    @Test
    void answersServerRequestAndPreservesStringId() {
        CapturingTransport transport = new CapturingTransport();
        transport.setMessageHandlers(ignored -> {
        }, message -> JsonUtils.json2Node("{\"roots\":[]}"));

        transport.handle(JsonUtils.json2Node("{\"jsonrpc\":\"2.0\",\"id\":\"server-1\",\"method\":\"roots/list\"}"));

        assertEquals("server-1", transport.sent.get().get("id").asText());
        assertEquals(0, transport.sent.get().get("result").get("roots").size());
    }

    @Test
    void convertsServerRequestHandlerFailureToJsonRpcError() {
        CapturingTransport transport = new CapturingTransport();
        transport.setMessageHandlers(ignored -> {
        }, message -> {
            throw new IllegalStateException("handler bug");
        });

        transport.handle(JsonUtils.json2Node(
                "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"sampling/createMessage\"}"));

        assertEquals(7, transport.sent.get().get("id").asInt());
        assertEquals(-32603, transport.sent.get().get("error").get("code").asInt());
        assertEquals("Client request handler failed",
                transport.sent.get().get("error").get("message").asText());
    }

    @Test
    void initializationHelperWaitsForTheNotificationAndKeepsTheOriginalResponse() {
        CapturingTransport transport = new CapturingTransport();
        CompletableFuture<JsonNode> initializeResponse = new CompletableFuture<>();
        AtomicInteger notificationCount = new AtomicInteger();

        CompletableFuture<JsonNode> completed = transport.completeInitializationForTest(initializeResponse,
                () -> {
                    assertEquals("2025-06-18", transport.getNegotiatedProtocolVersion());
                    notificationCount.incrementAndGet();
                    return CompletableFuture.completedFuture(null);
                });
        initializeResponse.complete(JsonUtils.json2Node("{\"jsonrpc\":\"2.0\",\"id\":9,\"result\":{\"protocolVersion\":\"2025-06-18\"}}"));

        assertEquals(1, notificationCount.get());
        assertEquals(9, completed.join().get("id").asInt());
    }

    @Test
    void unsupportedVersionNeverSendsInitializedNotification() {
        CapturingTransport transport = new CapturingTransport();
        AtomicInteger notifications = new AtomicInteger();
        CompletableFuture<JsonNode> result = transport.completeInitializationForTest(
                CompletableFuture.completedFuture(JsonUtils.json2Node("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"2099-01-01\"}}")),
                () -> { notifications.incrementAndGet(); return CompletableFuture.completedFuture(null); });
        org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
        assertEquals(0, notifications.get());
    }

    /**
     * Represents capturing transport.
     */
    private static final class CapturingTransport extends McpTransport {
        /**
         * Holds the sent value.
         */
        final AtomicReference<JsonNode> sent = new AtomicReference<>();

        CompletableFuture<JsonNode> completeInitializationForTest(CompletableFuture<JsonNode> response,
                                                                   java.util.function.Supplier<CompletableFuture<JsonNode>> notification) {
            return completeInitialization(response, notification);
        }

        @Override
        public void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
            setPendingRequests(pendingRequest);
        }

        @Override
        public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
            return new CompletableFuture<>();
        }

        @Override
        public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
            return new CompletableFuture<>();
        }

        @Override
        public void sendRequestWithoutResponse(McpRequest request) {
        }

        @Override
        protected void sendJson(JsonNode message) {
            sent.set(message);
        }

        @Override
        public void checkHealth() {
        }

        @Override
        public void close() {
        }
    }
}
