package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpClientToolErrorTest {
    @Test
    void legacyListToolsPropagatesJsonRpcError() {
        ErrorTransport transport = new ErrorTransport();
        McpClient client = McpClient.builder().transport(transport).build();
        transport.start(client.pendingRequests);

        McpException error = assertThrows(McpException.class, () -> client.listTools(0));

        assertEquals(-32602, error.getErrorCode());
    }

    private static final class ErrorTransport extends McpTransport {
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
            ObjectNode response = JsonNodeFactory.instance.objectNode();
            response.putObject("error").put("code", -32602).put("message", "bad cursor");
            return CompletableFuture.completedFuture(response);
        }

        @Override
        public void sendRequestWithoutResponse(McpRequest request) {
        }

        @Override
        public void checkHealth() {
        }

        @Override
        public void close() {
        }
    }
}
