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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void zeroTimeoutMeansUnlimitedWaitForHealthCheck() {
        CompletableFuture<JsonNode> response = new CompletableFuture<>();
        TestTransport transport = new TestTransport(response);
        McpClient client = McpClient.builder()
                .transport(transport)
                .requestTimeout(Duration.ZERO)
                .build();
        transport.start(client.pendingRequests);

        Thread completer = new Thread(() -> {
            try {
                Thread.sleep(50);
                response.complete(JsonNodeFactory.instance.objectNode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "timeout-test-completer");
        completer.setDaemon(true);
        completer.start();

        assertDoesNotThrow(client::checkHealth);
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

    private static class TestTransport extends McpTransport {
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
