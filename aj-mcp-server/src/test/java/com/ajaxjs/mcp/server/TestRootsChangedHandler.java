package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.model.HttpResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TestRootsChangedHandler {
    private static final String CHANGED = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/roots/list_changed\"}";
    private McpServer server;
    private ServerStreamableHttp transport;

    @BeforeEach
    void setup() {
        server = new McpServer();
        ServerConfig config = new ServerConfig();
        config.setName("roots-test");
        config.setVersion("1");
        server.setServerConfig(config);
        transport = new ServerStreamableHttp(server);
        server.setTransport(transport);
    }

    @AfterEach
    void close() throws Exception { transport.close(); }

    private String initialize(String version, String capabilities) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\""
                + version + "\",\"capabilities\":" + capabilities
                + ",\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}";
    }

    private void dispatch(String json) {
        com.fasterxml.jackson.databind.JsonNode node = JsonUtils.json2Node(json);
        server.processMessage(new McpRequestRawInfo(node.has("id") ? node.get("id").asInt() : null,
                node.get("method").asText(), node));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-11-05", "2025-03-26", "2025-06-18"})
    void sharedDispatcherSupportsAllVersionsAndSessions(String version) {
        List<String> notified = new ArrayList<>();
        server.setRootsChangedHandler(notified::add);
        try {
            for (String id : Arrays.asList("first", "second")) {
                server.bindSession(id);
                dispatch(initialize(version, "{\"roots\":{\"listChanged\":true}}"));
                dispatch("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
                assertNull(server.processMessage(new McpRequestRawInfo(null,
                        "notifications/roots/list_changed", JsonUtils.json2Node(CHANGED))));
            }
        } finally { server.clearSession(); }
        assertEquals(Arrays.asList("first", "second"), notified);
    }

    private Map<String, String> session(String capabilities) {
        HttpResult initialized = transport.post(initialize("2025-06-18", capabilities), Collections.emptyMap());
        Map<String, String> headers = new HashMap<>(initialized.getHeaders());
        headers.put("MCP-Protocol-Version", "2025-06-18");
        assertEquals(202, transport.post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", headers).getStatus());
        return headers;
    }

    private void notify(Map<String, String> headers) {
        HttpResult result = transport.post(CHANGED, headers);
        assertEquals(202, result.getStatus());
        assertNull(result.getBody());
    }

    @Test
    void callbacksAreOptionalAndFailuresDoNotBreakReception() {
        Map<String, String> headers = session("{\"roots\":{\"listChanged\":true}}");
        notify(headers);
        server.setRootsChangedHandler(id -> { throw new IllegalStateException("business failure"); });
        notify(headers);
        List<String> notified = new ArrayList<>();
        server.setRootsChangedHandler(notified::add);
        notify(headers);
        server.setRootsChangedHandler(null);
        notify(headers);
        assertEquals(Collections.singletonList(headers.get("Mcp-Session-Id")), notified);
    }

    @Test
    void ignoresUnadvertisedChangesAndRejectsRequestsWithIds() {
        List<String> notified = new ArrayList<>();
        server.setRootsChangedHandler(notified::add);
        notify(session("{}"));
        notify(session("{\"roots\":{\"listChanged\":false}}"));
        Map<String, String> headers = session("{\"roots\":{\"listChanged\":true}}");
        HttpResult result = transport.post(CHANGED.replace("\"method\"", "\"id\":9,\"method\""), headers);
        assertEquals(-32600, JsonUtils.json2Node(result.getBody()).path("error").path("code").asInt());
        assertTrue(notified.isEmpty());
    }
}
