package com.foo.streamable;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.client.transport.StreamableHttpTransport;
import com.ajaxjs.mcp.protocol.client.Root;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.utils.RequestMeta;
import com.ajaxjs.mcp.server.McpServer;
import okhttp3.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real embedded Tomcat and real SDK clients, not mocked controller dispatch. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Timeout(20)
class StreamableHttpSampleTest {
    @LocalServerPort int port;
    @Autowired McpServer server;

    private String endpoint() { return "http://127.0.0.1:" + port + "/mcp"; }

    @ParameterizedTest @ValueSource(strings = {"2025-03-26", "2025-06-18"})
    void fullRoundTripAndDelete(String version) throws Exception {
        StreamableHttpTransport transport = new StreamableHttpTransport(endpoint(), true, Duration.ofSeconds(5), null);
        String session;
        try (McpClient client = McpClient.builder().transport(transport).protocolVersion(version).build()) {
            CountDownLatch updates = new CountDownLatch(3);
            client.onNotification("notifications/progress", message -> {
                if (message.path("message").asText().startsWith("Completed step")) updates.countDown();
            });
            client.setRoots(Collections.singletonList(new Root("file:///sample-workspace", "workspace")), false);
            client.initialize(); transport.getEventStreamReady().get(5, TimeUnit.SECONDS);
            session = transport.getSessionId();
            assertEquals(version, client.getNegotiatedProtocolVersion());
            assertEquals(4, client.listTools().size());
            assertTrue(client.listTools().stream().allMatch(tool -> tool.getInputSchema() != null));
            assertEquals("Hello, world!", client.callTool(new CallToolRequest("greet")));
            assertEquals("Hello, Ada!", client.callTool("greet", "{\"name\":\"Ada\"}"));
            assertFalse(client.callTool(new CallToolRequest("serverTime")).isEmpty());
            CallToolRequest work = new CallToolRequest("progressDemo");
            work.getParams().setMeta(new RequestMeta("test-progress"));
            assertEquals("Completed 3 steps", client.callTool(work));
            assertTrue(updates.await(3, TimeUnit.SECONDS));
            assertTrue(client.callTool(new CallToolRequest("clientRoots")).contains("file:///sample-workspace"));
            assertEquals(1, client.listResources().size());
            assertTrue(client.readResource("demo://welcome").toString().contains("Welcome"));
            assertEquals(1, client.listPrompts().size());
            assertTrue(client.getPrompt("explain", "{\"topic\":\"MCP\"}").toString().contains("Briefly explain MCP"));
        }
        assertNull(server.getNegotiatedProtocolVersion(session), "close sends DELETE before returning");
        assertTrue(server.getPendingClientResponses().isEmpty());
    }

    @Test void httpStatusHeadersAndNotificationBodies() throws Exception {
        OkHttpClient http = new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(5)).build();
        try {
            try (Response response = http.newCall(new Request.Builder().url(endpoint()).header("Origin", "https://untrusted.example").build()).execute()) {
                assertEquals(403, response.code());
            }
            try (Response response = http.newCall(new Request.Builder().url(endpoint()).header("Mcp-Session-Id", "unknown").build()).execute()) {
                assertEquals(404, response.code());
            }
            StreamableHttpTransport transport = new StreamableHttpTransport(endpoint(), false, Duration.ofSeconds(5), null);
            try (McpClient client = McpClient.builder().transport(transport).protocolVersion("2025-06-18").build()) {
                client.initialize();
                String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
                Request.Builder request = new Request.Builder().url(endpoint()).header("Mcp-Session-Id", transport.getSessionId())
                        .header("Content-Type", "application/json").post(RequestBody.create(notification.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                try (Response response = http.newCall(request.build()).execute()) { assertEquals(400, response.code()); }
                request.header("MCP-Protocol-Version", "2025-06-18");
                try (Response response = http.newCall(request.build()).execute()) {
                    assertEquals(202, response.code()); assertEquals("", response.body().string());
                }
            }
        } finally {
            http.dispatcher().cancelAll(); http.dispatcher().executorService().shutdown(); http.connectionPool().evictAll();
        }
    }
}
