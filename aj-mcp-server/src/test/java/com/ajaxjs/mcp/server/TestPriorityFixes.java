package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.client.Root;
import com.ajaxjs.mcp.protocol.utils.completion.CompleteRequest;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage through the actual HTTP adapter and invocation boundaries. */
class TestPriorityFixes {
    private McpServer server;
    private ServerStreamableHttp transport;

    @BeforeEach
    void setup() {
        server = new McpServer();
        ServerConfig config = new ServerConfig(); config.setName("test"); config.setVersion("1");
        server.setServerConfig(config);
        server.getFeatureMgr().init("com.ajaxjs.mcp.server.hardening");
        transport = new ServerStreamableHttp(server); server.setTransport(transport);
    }

    @AfterEach
    void close() throws Exception { transport.close(); }

    private Map<String,String> session() {
        String init = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{\"roots\":{}},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}";
        ServerStreamableHttp.HttpResult result = transport.post(init, Collections.emptyMap());
        Map<String,String> headers = new HashMap<>(result.getHeaders());
        headers.put("MCP-Protocol-Version", "2025-06-18");
        assertEquals(202, transport.post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", headers).getStatus());
        return headers;
    }

    @Test
    void routesReverseResponseThroughHttpPost() throws Exception {
        Map<String,String> h = session(); String sid = h.get("Mcp-Session-Id");
        CompletableFuture<JsonNode> outgoing = new CompletableFuture<>();
        PrintWriter writer = new PrintWriter(new StringWriter()) {
            @Override public void write(String frame) {
                if (frame.contains("data: ")) outgoing.complete(JsonUtils.json2Node(frame.substring(frame.indexOf("data: ") + 6).trim()));
            }
        };
        transport.openEventStream(sid, writer, h);
        CompletableFuture<List<Root>> call = CompletableFuture.supplyAsync(() -> server.listRoots(sid, Duration.ofSeconds(2)));
        long id = outgoing.get(1, TimeUnit.SECONDS).get("id").asLong();
        ServerStreamableHttp.HttpResult accepted = transport.post("{\"jsonrpc\":\"2.0\",\"id\":"+id+",\"result\":{\"roots\":[{\"uri\":\"file:///workspace\"}]}}", h);
        assertEquals(202, accepted.getStatus()); assertNull(accepted.getBody());
        assertEquals("file:///workspace", call.get(1, TimeUnit.SECONDS).get(0).getUri());
        assertTrue(server.getPendingClientResponses().isEmpty());
    }

    @Test
    void notificationFailureHasNoResponse() {
        Map<String,String> h = session();
        ServerStreamableHttp.HttpResult result = transport.post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", h);
        assertEquals(202, result.getStatus()); assertNull(result.getBody());
    }

    @Test
    void closesSessionsWithoutGetAndRejectsOpeningAfterClose() throws Exception {
        Map<String,String> h = session(); String sid = h.get("Mcp-Session-Id");
        CompletableFuture<JsonNode> pending = new CompletableFuture<>(); server.getPendingClientResponses().put(sid+":1", pending);
        transport.close();
        assertNull(server.getNegotiatedProtocolVersion(sid)); assertTrue(pending.isCompletedExceptionally());
        assertEquals(503, transport.openEventStream(sid, new PrintWriter(new StringWriter()), h).getStatus());
    }

    @Test
    void expiresIdleSessionWithoutGet() {
        String sid = session().get("Mcp-Session-Id");
        transport.maintainSessions(System.nanoTime() + Duration.ofMinutes(31).toNanos());
        assertNull(server.getNegotiatedProtocolVersion(sid));
    }

    @Test
    void oldDisconnectDoesNotCloseReplacementAndHeartbeatDetectsBrokenWriter() {
        Map<String,String> h = session(); String sid = h.get("Mcp-Session-Id");
        PrintWriter old = new PrintWriter(new StringWriter());
        transport.openEventStream(sid, old, h);
        StringWriter live = new StringWriter(); PrintWriter current = new PrintWriter(live);
        transport.openEventStream(sid, current, h);
        transport.closeEventStream(sid, old);
        transport.send(sid, "{}"); assertTrue(live.toString().contains("data: {}"));
        current.close();
        CompletableFuture<JsonNode> pending = new CompletableFuture<>(); server.getPendingClientResponses().put(sid+":9", pending);
        transport.maintainSessions(System.nanoTime());
        assertTrue(pending.isCompletedExceptionally());
        assertThrows(IllegalStateException.class, () -> transport.send(sid, "{}"));
        assertNotNull(server.getNegotiatedProtocolVersion(sid), "GET disconnect must allow reconnection");
    }

    @Test
    void nullAndZeroReverseTimeoutUseFiniteConfiguredDefault() {
        Map<String,String> h = session(); String sid = h.get("Mcp-Session-Id");
        transport.openEventStream(sid, new PrintWriter(new StringWriter()), h);
        server.getServerConfig().setClientRequestTimeout(Duration.ofMillis(20));
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertThrows(RuntimeException.class, () -> server.listRoots(sid, null));
            assertThrows(RuntimeException.class, () -> server.listRoots(sid, Duration.ZERO));
        });
        assertTrue(server.getPendingClientResponses().isEmpty());
    }

    @Test
    void numericErrorsKeepRequestIdAndNeverInvokeWithTruncatedValues() {
        Map<String,String> h = session();
        for (String value : Arrays.asList("1.9", "2147483648", "-2147483649")) {
            JsonNode response = JsonUtils.json2Node(transport.post("{\"jsonrpc\":\"2.0\",\"id\":42,\"method\":\"tools/call\",\"params\":{\"name\":\"integer\",\"arguments\":{\"count\":"+value+"}}}",h).getBody());
            assertEquals(-32602,response.path("error").path("code").asInt()); assertEquals(42,response.path("id").asInt());
        }
        assertEquals(Integer.MAX_VALUE,McpServer.convertToType(2147483647L,int.class));
        assertEquals(Long.MAX_VALUE,McpServer.convertToType("9223372036854775807",long.class));
        assertThrows(IllegalArgumentException.class, () -> McpServer.convertToType("1e999",double.class));
        assertEquals("integer",server.getFeatureMgr().getToolStore().get("integer").getTool().getInputSchema().getProperties().get("count").getType());
    }

    @Test
    void completionContextRoundTripsAndReachesProvider() {
        CompleteRequest.Params params = new CompleteRequest.Params(); params.setContext(Collections.singletonMap("language","python"));
        JsonNode wire = JsonUtils.valueToTree(params);
        assertEquals("python",wire.path("context").path("arguments").path("language").asText());
        assertFalse(wire.has("wireContext"));
        assertEquals(params.getContext(),JsonUtils.convertValue(wire,CompleteRequest.Params.class).getContext());
        String body = transport.post("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"completion/complete\",\"params\":{\"ref\":{\"type\":\"ref/prompt\",\"name\":\"framework\"},\"argument\":{\"name\":\"framework\",\"value\":\"fl\"},\"context\":{\"arguments\":{\"language\":\"python\"}}}}",session()).getBody();
        assertTrue(body.contains("python:fl"),body);
    }

    @Test
    void loggingFiltersEachSessionIndependently() {
        Map<String,String> a=session(), b=session(); StringWriter aw=new StringWriter(), bw=new StringWriter();
        transport.openEventStream(a.get("Mcp-Session-Id"),new PrintWriter(aw),a);
        transport.openEventStream(b.get("Mcp-Session-Id"),new PrintWriter(bw),b);
        transport.post("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"logging/setLevel\",\"params\":{\"level\":\"error\"}}",a);
        server.publishLog("info", "test", "info-only"); server.publishLog("error","test","both");
        assertFalse(aw.toString().contains("info-only")); assertTrue(bw.toString().contains("info-only"));
        assertTrue(aw.toString().contains("both")); assertTrue(bw.toString().contains("both"));
        transport.delete(a.get("Mcp-Session-Id"),a); assertFalse(server.getSessionLoggingLevels().containsKey(a.get("Mcp-Session-Id")));
    }

    @Test
    void stdioUsesUtf8AndBoundedExecutor() throws Exception {
        InputStream savedIn=System.in; PrintStream savedOut=System.out;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        String request="{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"\u4f60\u597d\uD83D\uDE00\"}}}\n";
        server.getServerConfig().setStrictLifecycle(false); server.getServerConfig().setStdioWorkers(1); server.getServerConfig().setStdioQueueCapacity(2);
        try {
            System.setIn(new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(output,true,"UTF-8"));
            ServerStdio stdio=new ServerStdio(server);
            try {
                ThreadPoolExecutor executor=(ThreadPoolExecutor)stdio.getRequestExecutor();
                assertEquals(1,executor.getMaximumPoolSize()); assertEquals(2,executor.getQueue().remainingCapacity());
                stdio.start();
            } finally {stdio.close();}
        } finally {System.setIn(savedIn);System.setOut(savedOut);}
        assertTrue(new String(output.toByteArray(),StandardCharsets.UTF_8).contains("\u4f60\u597d\uD83D\uDE00"));
    }

    @Test
    void saturatedStdioStillAcceptsPingAndCancellation() throws Exception {
        InputStream savedIn=System.in; PrintStream savedOut=System.out;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        server.getServerConfig().setStrictLifecycle(false);server.getServerConfig().setStdioWorkers(1);server.getServerConfig().setStdioQueueCapacity(1);
        server.getFeatureMgr().init("com.ajaxjs.mcp.server.cancellation");
        com.ajaxjs.mcp.server.cancellation.CancellableTools.reset(1);
        try {
            System.setIn(new ByteArrayInputStream(new byte[0]));System.setOut(new PrintStream(output,true,"UTF-8"));
            ServerStdio stdio=new ServerStdio(server);
            try {
                java.lang.reflect.Method dispatch=ServerStdio.class.getDeclaredMethod("dispatchLine",String.class);dispatch.setAccessible(true);
                dispatch.invoke(stdio,"{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"blocking\"}}");
                assertTrue(com.ajaxjs.mcp.server.cancellation.CancellableTools.entered.await(1,TimeUnit.SECONDS));
                dispatch.invoke(stdio,"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
                dispatch.invoke(stdio,"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\"}");
                dispatch.invoke(stdio,"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"ping\"}");
                assertTrue(new String(output.toByteArray(),StandardCharsets.UTF_8).contains("busy"));
                assertTrue(new String(output.toByteArray(),StandardCharsets.UTF_8).contains("\"id\":4"));
                dispatch.invoke(stdio,"{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":1}}");
                stdio.getRequestExecutor().shutdown();assertTrue(stdio.getRequestExecutor().awaitTermination(2,TimeUnit.SECONDS));
                assertTrue(new String(output.toByteArray(),StandardCharsets.UTF_8).contains("\"isError\":true"));
            } finally {com.ajaxjs.mcp.server.cancellation.CancellableTools.release.countDown();stdio.close();}
        } finally {System.setIn(savedIn);System.setOut(savedOut);}
    }
}
