package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.McpClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Represents streamable http transport test.
 */
class StreamableHttpTransportTest {
    @Test
    void retainsSessionAndSendsNegotiatedVersionOnSubsequentPosts() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> versionHeader = new AtomicReference<>();
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/mcp", exchange -> respond(exchange, calls.incrementAndGet(), versionHeader));
        http.start();

        StreamableHttpTransport transport = StreamableHttpTransport.builder()
                .endpointUrl("http://127.0.0.1:" + http.getAddress().getPort() + "/mcp")
                .openEventStream(false)
                .build();
        McpClient client = McpClient.builder().transport(transport)
                .protocolVersion("2025-06-18").build();
        try {
            client.initialize();
            client.checkHealth();
            assertEquals("session-1", transport.getSessionId());
            assertEquals("2025-06-18", versionHeader.get());
            assertEquals(3, calls.get());
        } finally {
            client.close();
            http.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, int call,
                                AtomicReference<String> versionHeader) throws IOException {
        String request = read(exchange.getRequestBody());
        if (call == 1) {
            byte[] response = ("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{" +
                    "\"protocolVersion\":\"2025-06-18\",\"capabilities\":{}," +
                    "\"serverInfo\":{\"name\":\"test\",\"version\":\"1\"}}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add(StreamableHttpTransport.SESSION_ID_HEADER, "session-1");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
        } else if (request.contains("notifications/initialized")) {
            versionHeader.set(exchange.getRequestHeaders().getFirst(StreamableHttpTransport.PROTOCOL_VERSION_HEADER));
            exchange.sendResponseHeaders(202, -1);
        } else {
            versionHeader.set(exchange.getRequestHeaders().getFirst(StreamableHttpTransport.PROTOCOL_VERSION_HEADER));
            byte[] response = "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":{}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
        }
        exchange.close();
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int count;
        while ((count = input.read(buffer)) != -1)
            output.write(buffer, 0, count);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }
}
