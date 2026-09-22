package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.client.ElicitRequestParams;
import com.ajaxjs.mcp.protocol.client.ElicitResult;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.model.HttpResult;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Represents test protocol versions.
 */
class TestProtocolVersions {
    /**
     * Holds the server value.
     */
    private McpServer server;
    /**
     * Holds the transport value.
     */
    private ServerStreamableHttp transport;

    @AfterEach
    void close() throws Exception {
        transport.close();
    }

    @BeforeEach
    void setUp() {
        FeatureMgr features = new FeatureMgr();
        features.init("com.ajaxjs.mcp.server.advanced");
        server = new McpServer();
        server.setFeatureMgr(features);
        ServerConfig config = new ServerConfig();
        config.setName("test");
        config.setVersion("1");
        server.setServerConfig(config);
        transport = new ServerStreamableHttp(server);
        server.setTransport(transport);
    }

    @Test
    void deleteValidatesVersionBeforeRemovingSessionAndInitializeRejectsUnknownHeader() {
        HttpResult init = initialize("2025-06-18", false);
        String sid = init.getHeaders().get(ServerStreamableHttp.SESSION_ID_HEADER);
        Map<String, String> headers = sessionHeaders(sid, "2025-06-18");
        headers.put(ServerStreamableHttp.PROTOCOL_VERSION_HEADER, "unknown");
        assertEquals(400, transport.delete(sid, headers).getStatus());
        assertNotNull(server.getNegotiatedProtocolVersion(sid));
        headers.remove(ServerStreamableHttp.PROTOCOL_VERSION_HEADER);
        assertEquals(400, transport.delete(sid, headers).getStatus());
        headers.put(ServerStreamableHttp.PROTOCOL_VERSION_HEADER, "2025-06-18");
        assertEquals(204, transport.delete(sid, headers).getStatus());
        assertNull(server.getNegotiatedProtocolVersion(sid));
        assertEquals(400, transport.post(initializeJson("2025-06-18", false),
                Collections.singletonMap(ServerStreamableHttp.PROTOCOL_VERSION_HEADER, "unknown")).getStatus());
    }

    @Test
    void serverTitleIsVersionGated() {
        server.getServerConfig().setTitle("Friendly server");
        assertEquals("Friendly server", JsonUtils.json2Node(initialize("2025-06-18", false).getBody())
                .path("result").path("serverInfo").path("title").asText());
        assertFalse(JsonUtils.json2Node(initialize("2025-03-26", false).getBody())
                .path("result").path("serverInfo").has("title"));
    }

    @Test
    void negotiates20250326AndRejectsBatch() {
        HttpResult initialized = initialize("2025-03-26", false);
        assertEquals(200, initialized.getStatus());
        assertTrue(initialized.getBody().contains("\"protocolVersion\":\"2025-03-26\""));
        assertNotNull(initialized.getHeaders().get(ServerStreamableHttp.SESSION_ID_HEADER));
        String session = initialized.getHeaders().get(ServerStreamableHttp.SESSION_ID_HEADER);
        Map<String, String> headers = sessionHeaders(session, "2025-03-26");
        transport.post(initializedNotification(), headers);
        String tools = transport.post("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}", headers).getBody();
        assertTrue(tools.contains("\"readOnlyHint\":true"), tools);
        JsonNode structured = null;
        for (JsonNode tool : JsonUtils.json2Node(tools).path("result").path("tools"))
            if ("structured".equals(tool.path("name").asText())) structured = tool;
        assertNotNull(structured);
        assertEquals("Weather result", structured.path("annotations").path("title").asText());
        assertFalse(structured.hasNonNull("title"));

        HttpResult batch = transport.post("[]", Collections.emptyMap());
        assertEquals(400, batch.getStatus());
        assertTrue(batch.getBody().contains("batching is not supported"));
    }

    @Test
    void progressMessageAndAnnotationsRespectNegotiatedVersion() {
        for (String version : com.ajaxjs.mcp.protocol.ProtocolVersion.supportedVersions()) {
            HttpResult init = initialize(version, false);
            String sid = init.getHeaders().get(ServerStreamableHttp.SESSION_ID_HEADER);
            Map<String, String> headers = sessionHeaders(sid, version);
            transport.post(initializedNotification(), headers);
            java.io.StringWriter output = new java.io.StringWriter();
            transport.openEventStream(sid, new java.io.PrintWriter(output), headers);
            server.bindSession(sid);
            try {
                server.sendProgress("token", 1, 2.0, "Halfway");
            } finally {
                server.clearSession();
            }
            String frame = output.toString();
            JsonNode params = JsonUtils.json2Node(frame.substring(frame.indexOf("data: ") + 6).trim()).path("params");
            assertEquals(1, params.path("progress").asInt());
            assertEquals(!"2024-11-05".equals(version), params.has("message"));
            if (params.has("message")) assertEquals("Halfway", params.path("message").asText());
        }
    }

    @Test
    void versionHeaderIsRequiredFor20250618AndStructuredOutputIsReturned() {
        HttpResult initialized = initialize("2025-06-18", true);
        String session = initialized.getHeaders().get(ServerStreamableHttp.SESSION_ID_HEADER);

        Map<String, String> noVersion = new HashMap<>();
        noVersion.put(ServerStreamableHttp.SESSION_ID_HEADER, session);
        assertEquals(400, transport.post(initializedNotification(), noVersion).getStatus());

        Map<String, String> headers = sessionHeaders(session, "2025-06-18");
        assertEquals(202, transport.post(initializedNotification(), headers).getStatus());
        String tools = transport.post("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\"}", headers).getBody();
        assertTrue(tools.contains("Weather result"), tools);
        assertTrue(tools.contains("outputSchema"), tools);
        HttpResult tool = transport.post(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"structured\"}}",
                headers);
        assertEquals(200, tool.getStatus());
        assertTrue(tool.getBody().contains("\"structuredContent\":{\"temperature\":21}"), tool.getBody());
    }

    @Test
    void rejectsStructuredOutputForOlderRevision() {
        HttpResult initialized = initialize("2025-03-26", false);
        String session = initialized.getHeaders().get(ServerStreamableHttp.SESSION_ID_HEADER);
        Map<String, String> headers = sessionHeaders(session, "2025-03-26");
        transport.post(initializedNotification(), headers);
        String body = transport.post(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"structured\"}}",
                headers).getBody();
        assertTrue(body.contains("Structured tool output requires MCP 2025-06-18"), body);
    }

    @Test
    void elicitsOnlyFromCapable20250618Client() {
        server.setTransport(new ElicitationLoopback(server));
        server.bindSession("client-a");

        try {
            server.processMessage(McpServerInitialize.jsonRpcValidate(initializeJson("2025-06-18", true)));
        } finally {
            server.clearSession();
        }

        ElicitRequestParams params = new ElicitRequestParams();
        params.setMessage("Name?");
        params.setRequestedSchema(Collections.<String, Object>singletonMap("type", "object"));
        ElicitResult result = server.elicit("client-a", params, Duration.ofSeconds(1));
        assertEquals("accept", result.getAction());
        assertEquals("Ada", result.getContent().get("name"));
    }

    private HttpResult initialize(String version, boolean elicitation) {
        return transport.post(initializeJson(version, elicitation), Collections.<String, String>emptyMap());
    }

    private static String initializeJson(String version, boolean elicitation) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{" +
                "\"protocolVersion\":\"" + version + "\",\"capabilities\":" +
                (elicitation ? "{\"elicitation\":{}}" : "{}") +
                ",\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}";
    }

    private static String initializedNotification() {
        return "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
    }

    private static Map<String, String> sessionHeaders(String session, String version) {
        Map<String, String> headers = new HashMap<>();
        headers.put(ServerStreamableHttp.SESSION_ID_HEADER, session);
        headers.put(ServerStreamableHttp.PROTOCOL_VERSION_HEADER, version);
        return headers;
    }

    /**
     * Represents elicitation loopback.
     */
    private static final class ElicitationLoopback implements McpTransportSync {
        /**
         * Holds the server value.
         */
        private final McpServer server;

        private ElicitationLoopback(McpServer server) {
            this.server = server;
        }

        @Override
        public void start() {
        }

        @Override
        public String handle(String rawJson) {
            return null;
        }

        @Override
        public void initialize() {
        }

        @Override
        public void close() {
        }

        @Override
        public void send(String sessionId, String json) {
            JsonNode request = JsonUtils.json2Node(json);
            assertEquals("elicitation/create", request.get("method").asText());
            server.acceptClientResponse(sessionId, "{\"jsonrpc\":\"2.0\",\"id\":"
                    + request.get("id").asLong()
                    + ",\"result\":{\"action\":\"accept\",\"content\":{\"name\":\"Ada\"}}}");
        }
    }
}
