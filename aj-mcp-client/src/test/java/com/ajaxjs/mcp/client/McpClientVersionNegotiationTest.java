package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpClientVersionNegotiationTest {
    @Test
    void acceptsAConfiguredServerFallbackAndStoresItOnTransport() {
        NegotiatingTransport transport = new NegotiatingTransport();
        McpClient client = McpClient.builder()
                .transport(transport)
                .protocolVersion("2025-06-18")
                .build();
        client.initialize();

        assertEquals("2025-03-26", client.getNegotiatedProtocolVersion());
        assertEquals("2025-03-26", transport.getNegotiatedProtocolVersion());
    }

    private static final class NegotiatingTransport extends McpTransport {
        @Override public void start(Map<Long, CompletableFuture<JsonNode>> pending) { setPendingRequests(pending); }

        @Override
        public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
            return CompletableFuture.completedFuture(JsonUtils.json2Node(
                    "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"serverInfo\":{\"name\":\"s\",\"version\":\"1\"}}}"));
        }

        @Override public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) { return new CompletableFuture<>(); }
        @Override public void sendRequestWithoutResponse(McpRequest request) { }
        @Override public void checkHealth() { }
        @Override public void close() { }
    }
}
