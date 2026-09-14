package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

/** Tests actual HTTP framing, handshake ordering and independent GET lifecycle. */
@Timeout(10)
class StreamableHttpPriorityTest {
    private HttpServer http;
    private ExecutorService executor;
    private StreamableHttpTransport transport;

    private void start(HttpHandler handler, boolean get) throws Exception {
        http=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newCachedThreadPool();http.setExecutor(executor);
        http.createContext("/mcp",handler);http.start();
        transport=StreamableHttpTransport.builder().endpointUrl("http://127.0.0.1:"+http.getAddress().getPort()+"/mcp")
                .timeout(Duration.ofSeconds(3)).openEventStream(get).build();
    }

    @AfterEach
    void close() {
        if(transport!=null)transport.close();
        if(http!=null)http.stop(0);
        if(executor!=null)executor.shutdownNow();
    }

    private static JsonNode body(HttpExchange e) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;
        while((n=e.getRequestBody().read(buffer))!=-1)bytes.write(buffer,0,n);
        return JsonUtils.json2Node(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
    }

    private static void reply(HttpExchange e,int status,String json) throws IOException {
        if(json==null){e.sendResponseHeaders(status,-1);e.close();return;}
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");
        e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);e.close();
    }

    private static void initializeReply(HttpExchange e) throws IOException {
        e.getResponseHeaders().set("Mcp-Session-Id","session");
        reply(e,200,"{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"serverInfo\":{\"name\":\"test\",\"version\":\"1\"}}}");
    }

    private static void await(CountDownLatch latch) throws IOException {
        try {if(!latch.await(4,TimeUnit.SECONDS))throw new IOException("test latch timed out");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
    }

    @Test
    void negotiatedVersionPrecedesInitializedAndDelete() throws Exception {
        AtomicReference<String> initializedVersion=new AtomicReference<>(), deleteVersion=new AtomicReference<>();
        AtomicInteger deletes=new AtomicInteger();
        start(e->{
            if("DELETE".equals(e.getRequestMethod())){deleteVersion.set(e.getRequestHeaders().getFirst("MCP-Protocol-Version"));deletes.incrementAndGet();reply(e,204,null);return;}
            JsonNode request=body(e);
            if("initialize".equals(request.path("method").asText()))initializeReply(e);
            else {initializedVersion.set(e.getRequestHeaders().getFirst("MCP-Protocol-Version"));reply(e,"2025-06-18".equals(initializedVersion.get())?202:400,null);}
        },false);
        McpClient client=McpClient.builder().transport(transport).protocolVersion("2025-03-26").build();
        client.initialize();assertEquals("2025-06-18",client.getNegotiatedProtocolVersion());
        assertEquals("2025-06-18",initializedVersion.get());client.close();client.close();
        assertEquals(1,deletes.get());assertEquals("2025-06-18",deleteVersion.get());
    }

    @Test
    void dispatchesReverseRequestAndProgressBeforePostStreamCloses() throws Exception {
        CountDownLatch reverseReceived=new CountDownLatch(1), release=new CountDownLatch(1);
        CompletableFuture<JsonNode> progress=new CompletableFuture<>();
        start(e->{
            JsonNode request=body(e);
            if(request.has("result")){reverseReceived.countDown();reply(e,202,null);return;}
            e.getResponseHeaders().set("Content-Type","text/event-stream");e.sendResponseHeaders(200,0);
            e.getResponseBody().write("data: {\"jsonrpc\":\"2.0\",\"id\":\"reverse\",\"method\":\"roots/list\"}\n\n".getBytes(StandardCharsets.UTF_8));e.getResponseBody().flush();
            await(reverseReceived);
            e.getResponseBody().write(("data: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/progress\",\"params\":{\"progress\":1}}\n\n"+
                    "data: {\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{}}\n\n").getBytes(StandardCharsets.UTF_8));e.getResponseBody().flush();
            try{await(release);}finally{e.close();}
        },false);
        transport.start(new ConcurrentHashMap<>());transport.markInitialized();
        transport.setMessageHandlers(progress::complete,request->JsonUtils.json2Node("{\"roots\":[]}"));
        PingRequest ping=new PingRequest();ping.setId(2L);
        try {
            CompletableFuture<JsonNode> response=transport.sendRequestWithResponse(ping);
            assertEquals(1,progress.get(2,TimeUnit.SECONDS).path("params").path("progress").asInt());
            assertEquals(2,response.get(2,TimeUnit.SECONDS).path("id").asInt());
        } finally {release.countDown();}
    }

    @Test
    void get405DoesNotFailConcurrentPost() throws Exception {
        CountDownLatch pingEntered=new CountDownLatch(1),release=new CountDownLatch(1);
        start(e->{
            if("GET".equals(e.getRequestMethod())){await(pingEntered);reply(e,405,null);return;}
            if("DELETE".equals(e.getRequestMethod())){reply(e,204,null);return;}
            JsonNode request=body(e);String method=request.path("method").asText();
            if("initialize".equals(method))initializeReply(e);
            else if("notifications/initialized".equals(method))reply(e,202,null);
            else {pingEntered.countDown();await(release);reply(e,200,"{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{}}");}
        },true);
        McpClient client=McpClient.builder().transport(transport).protocolVersion("2025-06-18").build();client.initialize();
        PingRequest ping=new PingRequest();ping.setId(2L);CompletableFuture<JsonNode> response=transport.sendRequestWithResponse(ping);
        try {
            assertThrows(ExecutionException.class,()->transport.getEventStreamReady().get(2,TimeUnit.SECONDS));
            assertFalse(response.isDone());assertNotNull(transport.getEventStreamFailure());
            release.countDown();assertEquals(2,response.get(2,TimeUnit.SECONDS).path("id").asInt());
        } finally {release.countDown();}
    }

    @Test
    void reconnectsClosedGetWithLastEventId() throws Exception {
        AtomicInteger connections=new AtomicInteger();CompletableFuture<String> resumed=new CompletableFuture<>();
        CountDownLatch release=new CountDownLatch(1);
        start(e->{
            if("DELETE".equals(e.getRequestMethod())){reply(e,204,null);return;}
            if("GET".equals(e.getRequestMethod())) {
                int connection=connections.incrementAndGet();e.getResponseHeaders().set("Content-Type","text/event-stream");e.sendResponseHeaders(200,0);
                if(connection==1){e.getResponseBody().write("id: 42\ndata: {\"jsonrpc\":\"2.0\",\"method\":\"notifications/message\",\"params\":{}}\n\n".getBytes(StandardCharsets.UTF_8));e.getResponseBody().flush();e.close();}
                else {resumed.complete(e.getRequestHeaders().getFirst("Last-Event-ID"));e.getResponseBody().write(": ready\n\n".getBytes(StandardCharsets.UTF_8));e.getResponseBody().flush();try{await(release);}finally{e.close();}}
                return;
            }
            JsonNode request=body(e);if("initialize".equals(request.path("method").asText()))initializeReply(e);else reply(e,202,null);
        },true);
        McpClient client=McpClient.builder().transport(transport).protocolVersion("2025-06-18").build();
        try {client.initialize();transport.getEventStreamReady().get(2,TimeUnit.SECONDS);assertEquals("42",resumed.get(3,TimeUnit.SECONDS));}
        finally {release.countDown();}
    }
}
