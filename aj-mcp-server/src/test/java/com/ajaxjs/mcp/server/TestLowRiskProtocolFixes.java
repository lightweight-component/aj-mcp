package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.common.ContentEmbeddedResource;
import com.ajaxjs.mcp.protocol.common.ContentEmbeddedResourceDetail;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestLowRiskProtocolFixes {
    private McpServer server;

    @BeforeEach
    void setUp() {
        server = new McpServer();
        ServerConfig config = new ServerConfig();
        config.setStrictLifecycle(false);
        server.setServerConfig(config);
    }

    @Test
    void rejectsNonStringJsonRpcAndMethodFields() {
        assertInvalidRequest("{\"jsonrpc\":2.0,\"id\":1,\"method\":\"ping\"}");
        assertInvalidRequest("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":123}");
        assertInvalidRequest("{\"jsonrpc\":\"2.0\",\"id\":1}");
    }

    @Test
    void rejectsMalformedInitializeParamsAsInvalidParams() {
        assertInvalidInitialize("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}");
        assertInvalidInitialize(initializeWith("\"protocolVersion\":1,\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}"));
        assertInvalidInitialize(initializeWith("\"protocolVersion\":\"2025-06-18\",\"capabilities\":[],"
                + "\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}"));
        assertInvalidInitialize(initializeWith("\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"\",\"version\":\"1\"}"));
    }

    @Test
    void embeddedResourceSupportsBlob() {
        ContentEmbeddedResourceDetail resource = new ContentEmbeddedResourceDetail();
        resource.setUri("file:///binary");
        resource.setMimeType("application/octet-stream");
        resource.setBlob("AQID");

        String json = JsonUtils.toJson(resource);

        assertTrue(json.contains("\"blob\":\"AQID\""), json);
    }

    @Test
    void unsubscribeAndSessionRemovalDeleteEmptySubscriptionSets() {
        subscribe("session-a", "file:///one", true);
        subscribe("session-a", "file:///one", false);
        assertFalse(server.getResourceSubscriptions().containsKey("file:///one"));

        subscribe("session-a", "file:///two", true);
        server.removeSession("session-a");
        assertFalse(server.getResourceSubscriptions().containsKey("file:///two"));
    }

    private void subscribe(String sessionId, String uri, boolean subscribe) {
        String method = subscribe ? "resources/subscribe" : "resources/unsubscribe";
        server.bindSession(sessionId);
        try {
            McpResponse ignored = server.processMessage(McpServerInitialize.jsonRpcValidate(
                    "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method
                            + "\",\"params\":{\"uri\":\"" + uri + "\"}}"));
        } finally {
            server.clearSession();
        }
    }

    private static void assertInvalidRequest(String json) {
        JsonRpcErrorException error = assertThrows(JsonRpcErrorException.class,
                () -> McpServerInitialize.jsonRpcValidate(json));
        assertTrue(error.toJson().contains("\"code\":" + JsonRpcErrorCode.INVALID_REQUEST.getCode()));
    }

    private void assertInvalidInitialize(String json) {
        JsonRpcErrorException error = assertThrows(JsonRpcErrorException.class,
                () -> server.processMessage(McpServerInitialize.jsonRpcValidate(json)));
        assertTrue(error.toJson().contains("\"code\":" + JsonRpcErrorCode.INVALID_PARAMS.getCode()));
    }

    private static String initializeWith(String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{" + params + "}}";
    }
}
