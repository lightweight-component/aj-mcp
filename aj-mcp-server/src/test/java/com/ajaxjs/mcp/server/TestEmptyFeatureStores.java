package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestEmptyFeatureStores {
    @Test
    void listOperationsReturnEmptyLists() {
        McpServer server = new McpServer();

        assertEmptyList(server, "tools/list", "tools");
        assertEmptyList(server, "resources/list", "resources");
        assertEmptyList(server, "prompts/list", "prompts");
    }

    private static void assertEmptyList(McpServer server, String method, String field) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\"}";
        String response = JsonUtils.toJson(server.processMessage(McpServerInitialize.jsonRpcValidate(request)));

        assertTrue(response.contains("\"" + field + "\":[]"), response);
    }
}
