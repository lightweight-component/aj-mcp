package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseEventListenerPendingRequestsTest {
    @Test
    void failureCompletesAndRemovesAllPendingRequests() {
        TestTransport transport = new TestTransport();
        Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
        CompletableFuture<JsonNode> first = new CompletableFuture<>();
        CompletableFuture<JsonNode> second = new CompletableFuture<>();
        pending.put(1L, first);
        pending.put(2L, second);
        transport.start(pending);

        IOException failure = new IOException("connection lost");
        SseEventListener listener = new SseEventListener(
                transport, false, new CompletableFuture<>());
        listener.onFailure(null, failure, null);

        assertTrue(pending.isEmpty());
        assertSame(failure, assertFailure(first));
        assertSame(failure, assertFailure(second));
    }

    @Test
    void closedChannelCompletesPendingRequests() {
        TestTransport transport = new TestTransport();
        Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
        CompletableFuture<JsonNode> request = new CompletableFuture<>();
        pending.put(7L, request);
        transport.start(pending);

        SseEventListener listener = new SseEventListener(
                transport, false, new CompletableFuture<>());
        listener.onClosed(null);

        assertTrue(pending.isEmpty());
        assertEquals("SSE channel closed", assertFailure(request).getMessage());
    }

    private static Throwable assertFailure(CompletableFuture<JsonNode> future) {
        CompletionException exception = assertThrows(CompletionException.class, future::join);
        return exception.getCause();
    }

    private static class TestTransport extends McpTransport {
        @Override
        public void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
            setPendingRequests(pendingRequest);
        }

        @Override
        public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sendRequestWithoutResponse(McpRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void checkHealth() {
        }

        @Override
        public void close() {
        }
    }
}
