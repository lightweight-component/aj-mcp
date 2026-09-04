package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Represents mcp transport message routing test.
 */
class McpTransportMessageRoutingTest {
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

    /**
     * Represents capturing transport.
     */
    private static final class CapturingTransport extends McpTransport {
        /**
         * Holds the sent value.
         */
        final AtomicReference<JsonNode> sent = new AtomicReference<>();

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
