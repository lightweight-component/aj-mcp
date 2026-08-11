package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.client.Root;
import com.ajaxjs.mcp.protocol.client.SamplingCreateMessageParams;
import com.ajaxjs.mcp.protocol.client.SamplingCreateMessageResult;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestClientCapabilityRequests {
    @Test
    void serverCorrelatesRootsAndSamplingResponsesBySessionAndId() {
        McpServer server = new McpServer();
        server.setTransport(new LoopbackTransport(server));

        List<Root> roots = server.listRoots("client-a", Duration.ofSeconds(1));
        assertEquals("file:///workspace", roots.get(0).getUri());

        SamplingCreateMessageResult sampled = server.createMessage("client-a",
                new SamplingCreateMessageParams(), Duration.ofSeconds(1));
        assertEquals("test-model", sampled.getModel());
    }

    private static final class LoopbackTransport implements McpTransportSync {
        private final McpServer server;
        private LoopbackTransport(McpServer server) { this.server = server; }
        @Override public void start() { }
        @Override public String handle(String rawJson) { return null; }
        @Override public void initialize() { }
        @Override public void close() { }

        @Override
        public void send(String sessionId, String json) {
            JsonNode request = JsonUtils.json2Node(json);
            String result = "roots/list".equals(request.get("method").asText())
                    ? "{\"roots\":[{\"uri\":\"file:///workspace\",\"name\":\"workspace\"}]}"
                    : "{\"role\":\"assistant\",\"content\":{\"type\":\"text\",\"text\":\"ok\"},\"model\":\"test-model\",\"stopReason\":\"endTurn\"}";
            server.acceptClientResponse(sessionId, "{\"jsonrpc\":\"2.0\",\"id\":"
                    + request.get("id").asLong() + ",\"result\":" + result + "}");
        }
    }
}
