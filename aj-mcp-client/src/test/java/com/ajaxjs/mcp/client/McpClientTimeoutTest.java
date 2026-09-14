package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Represents mcp client timeout test.
 */
class McpClientTimeoutTest {
    @Test
    @Timeout(2)
    void initializationUsesConfiguredRequestTimeout() {
        TestTransport transport = new TestTransport(new CompletableFuture<JsonNode>());
        McpClient client = McpClient.builder()
                .transport(transport)
                .requestTimeout(Duration.ofMillis(20))
                .build();

        RuntimeException error = assertThrows(RuntimeException.class, client::initialize);

        assertInstanceOf(TimeoutException.class, error.getCause());
    }

    @Test
    @Timeout(2)
    void zeroAndNullTimeoutUseFiniteDefault() throws Exception {
        CompletableFuture<JsonNode> response = new CompletableFuture<JsonNode>() {
            @Override public JsonNode get() {
                throw new AssertionError("Unbounded waiting must not be used");
            }
            @Override public JsonNode get(long timeout, java.util.concurrent.TimeUnit unit) {
                assertEquals(60000, unit.toMillis(timeout));
                return JsonNodeFactory.instance.objectNode();
            }
        };
        McpClient.builder().requestTimeout(Duration.ZERO).build().awaitResponse(response);
        McpClient.builder().requestTimeout(null).build().awaitResponse(response);
    }

    @Test
    void negativeTimeoutIsRejected() {
        TestTransport transport = new TestTransport(
                CompletableFuture.completedFuture(JsonNodeFactory.instance.objectNode()));
        McpClient client = McpClient.builder()
                .transport(transport)
                .requestTimeout(Duration.ofMillis(-1))
                .build();
        transport.start(client.pendingRequests);

        assertThrows(IllegalArgumentException.class, client::checkHealth);
    }

    /**
     * Represents test transport.
     */
    private static class TestTransport extends McpTransport {
        /**
         * Holds the response value.
         */
        private final CompletableFuture<JsonNode> response;

        private TestTransport(CompletableFuture<JsonNode> response) {
            this.response = response;
        }

        @Override
        public void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
            setPendingRequests(pendingRequest);
        }

        @Override
        public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
            saveRequest(numericId(request.getId()), response);
            return response;
        }

        @Override
        public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
            saveRequest(numericId(request.getId()), response);
            return response;
        }

        @Override
        public void sendRequestWithoutResponse(McpRequest request) {
        }

        @Override
        public void checkHealth() {
        }

        @Override
        public void close() throws IOException {
        }
    }
}
