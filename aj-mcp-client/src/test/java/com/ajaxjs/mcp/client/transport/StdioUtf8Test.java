package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies actual child-process interoperability across a non-UTF-8 JVM default.
 */
class StdioUtf8Test {
    @Test
    void exchangesChineseAndEmojiWithUtf8Peer() throws Exception {
        StdioTransport transport = StdioTransport.builder().command(Arrays.asList(
                new File(System.getProperty("java.home"), "bin/java").getPath(), "-Dfile.encoding=GBK",
                "-cp", System.getProperty("java.class.path"), EchoPeer.class.getName())).build();
        try {
            transport.start(new ConcurrentHashMap<>());
            transport.markInitialized();
            McpRequest request = new McpRequest();
            request.setId(1L);
            request.setMethod("\u4f60\u597d\uD83D\uDE00");
            JsonNode response = transport.sendRequestWithResponse(request).get(3, TimeUnit.SECONDS);
            assertEquals(request.getMethod(), response.path("result").path("echo").asText());
        } finally {
            transport.close();
        }
    }

    /**
     * A standards-compliant UTF-8 peer running with a GBK platform default.
     */
    public static class EchoPeer {
        public static void main(String[] args) throws IOException {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
            JsonNode request = JsonUtils.json2Node(input.readLine());
            com.fasterxml.jackson.databind.node.ObjectNode response = JsonUtils.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("id", request.get("id"));
            response.putObject("result").set("echo", request.get("method"));
            output.println(response.toString());
            input.readLine();
        }
    }
}
