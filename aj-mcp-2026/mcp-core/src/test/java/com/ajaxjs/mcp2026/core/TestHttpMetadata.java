package com.ajaxjs.mcp2026.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static com.ajaxjs.mcp2026.core.TestMcpCodec.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies HTTP header/body metadata binding and header-value encoding.
 */
class TestHttpMetadata {
    ToolDefinition tool() {
        return ToolDefinition.create("echo", map("type", "object", "properties", map(
                "nested", map("type", "object", "properties", map("tenant", map("type", "string", "x-mcp-header", "Tenant"))),
                "count", map("type", "integer", "x-mcp-header", "Count"))));
    }

    RpcRequest request(Map<String, Object> args) {
        return RpcRequest.create(RequestId.of(1), "tools/call", map("name", "echo", "arguments", args), RequestMetadata.create(map()));
    }

    Map<String, List<String>> incoming(Map<String, String> headers) {
        Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((k, v) -> result.put(k, Collections.singletonList(v)));
        return result;
    }

    @Test
    void roundTripsUnicodeWhitespaceSentinelsAndControlCharacters() {
        for (String value : Arrays.asList("", "plain", "中文", " padded ", "line\nbreak", "=?base64?abc?=", "a\tb")) {
            assertEquals(value, HttpMetadata.decode(HttpMetadata.encode(value)));
            RpcRequest request = request(map("nested", map("tenant", value)));
            HttpMetadata.require(request, tool(), incoming(HttpMetadata.headers(request, tool())));
        }
    }

    @Test
    void checksMissingMismatchDuplicateNumericAndCaseInsensitiveNames() {
        RpcRequest request = request(map("count", 42));
        Map<String, List<String>> headers = incoming(HttpMetadata.headers(request, tool()));
        headers.put("mcp-param-count", Collections.singletonList("42.0"));
        HttpMetadata.require(request, tool(), headers);
        headers.put("Mcp-Param-Count", Collections.singletonList("43"));
        assertEquals(ErrorCodes.HEADER_MISMATCH, assertThrows(McpProtocolException.class,
                () -> HttpMetadata.require(request, tool(), headers)).getError().getCode());
        headers.put("Mcp-Param-Count", Arrays.asList("42", "42"));
        assertThrows(McpProtocolException.class, () -> HttpMetadata.require(request, tool(), headers));
        headers.remove("Mcp-Param-Count");
        assertThrows(McpProtocolException.class, () -> HttpMetadata.require(request, tool(), headers));
    }

    @Test
    void absentAndNullValuesAreOmittedButRecognizedExtraHeadersAreRejected() {
        RpcRequest request = request(map("count", null));
        Map<String, List<String>> headers = incoming(HttpMetadata.headers(request, tool()));
        assertFalse(headers.containsKey("Mcp-Param-Count"));
        HttpMetadata.require(request, tool(), headers);
        headers.put("Mcp-Param-Unknown", Collections.singletonList("ignored"));
        HttpMetadata.require(request, tool(), headers);
        headers.put("Mcp-Param-Count", Collections.singletonList("1"));
        assertThrows(McpProtocolException.class, () -> HttpMetadata.require(request, tool(), headers));
    }

    @Test
    void rejectsUnsafeIntegersAndIllegalAnnotationLocations() {
        assertThrows(IllegalArgumentException.class, () -> HttpMetadata.headers(request(map("count", 9007199254740992L)), tool()));
        for (String key : Arrays.asList("items", "allOf", "if", "$defs")) {
            ToolDefinition invalid = ToolDefinition.create("x", map("type", "object", key, map("type", "string", "x-mcp-header", "x")));
            assertThrows(IllegalArgumentException.class, () -> HttpMetadata.validateTool(invalid));
        }
        ToolDefinition duplicate = ToolDefinition.create("x", map("type", "object", "properties", map(
                "a", map("type", "string", "x-mcp-header", "X"), "b", map("type", "string", "x-mcp-header", "x"))));
        assertThrows(IllegalArgumentException.class, () -> HttpMetadata.validateTool(duplicate));
        for (String name : Arrays.asList("", "a b", "a\r\nb"))
            assertThrows(IllegalArgumentException.class, () ->
                    HttpMetadata.validateTool(ToolDefinition.create("x", map("type", "object", "properties", map(
                            "a", map("type", "string", "x-mcp-header", name))))));
    }

    @Test
    void boundedUtf8LinesHandleCrlfAndRejectMalformedBytes() throws Exception {
        assertEquals("中文", WireIO.line(new ByteArrayInputStream("中文\r\n".getBytes("UTF-8"))));
        assertNull(WireIO.line(new ByteArrayInputStream(new byte[0])));
        assertThrows(IOException.class, () -> WireIO.line(new ByteArrayInputStream(new byte[]{(byte) 0xff, 10})));
        byte[] huge = new byte[WireIO.MAX_BYTES + 1];
        assertThrows(IOException.class, () -> WireIO.readAll(new ByteArrayInputStream(huge)));
    }
}
