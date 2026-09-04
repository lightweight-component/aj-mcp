package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Represents test strict lifecycle.
 */
class TestStrictLifecycle {
    @Test
    void rejectsFeatureCallsUntilInitializedNotificationArrives() {
        McpServer server = new McpServer();
        server.setServerConfig(new ServerConfig());

        assertThrows(JsonRpcErrorException.class, () -> process(server,
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"));
        process(server, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"initialize\",\"params\":{" +
                "\"protocolVersion\":\"2024-11-05\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}");
        process(server, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        assertDoesNotThrow(() -> process(server,
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\"}"));
    }

    private static void process(McpServer server, String json) {
        server.processMessage(McpServerInitialize.jsonRpcValidate(json));
    }
}
