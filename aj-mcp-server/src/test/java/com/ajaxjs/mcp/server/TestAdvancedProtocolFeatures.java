package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Represents test advanced protocol features.
 */
class TestAdvancedProtocolFeatures {
    /**
     * Holds the server value.
     */
    private McpServer server;

    @BeforeEach
    void setUp() {
        FeatureMgr features = new FeatureMgr();
        features.init("com.ajaxjs.mcp.server.advanced");
        server = new McpServer();
        server.setFeatureMgr(features);
        ServerConfig config = new ServerConfig();
        config.setStrictLifecycle(false);
        server.setServerConfig(config);
    }

    @Test
    void preservesStringRequestId() {
        String json = response("{\"jsonrpc\":\"2.0\",\"id\":\"request-a\",\"method\":\"ping\"}");
        assertTrue(json.contains("\"id\":\"request-a\""), json);
    }

    @Test
    void omittedOptionalToolArgumentIsNull() {
        String json = response("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{" +
                "\"name\":\"optional\",\"arguments\":{\"required\":\"ok\"}}}");
        assertTrue(json.contains("\"text\":\"ok\""), json);
    }

    @Test
    void listsAndReadsResourceTemplate() {
        assertTrue(response("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"resources/templates/list\"}")
                .contains("users://{id}"));
        assertTrue(response("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"resources/read\",\"params\":{\"uri\":\"users://42\"}}")
                .contains("user=42"));
    }

    @Test
    void completesPromptArgument() {
        String json = response("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"completion/complete\",\"params\":{" +
                "\"ref\":{\"type\":\"ref/prompt\",\"name\":\"greet\"}," +
                "\"argument\":{\"name\":\"name\",\"value\":\"a\"}}}");
        assertTrue(json.contains("a-one"), json);
    }

    private String response(String request) {
        McpResponse response = server.processMessage(McpServerInitialize.jsonRpcValidate(request));
        return JsonUtils.toJson(response);
    }
}
