package com.ajaxjs.mcp2026.core;

import com.ajaxjs.mcp2026.core.json.McpJacksonEngine;
import com.ajaxjs.util.JsonUtil;
import com.ajaxjs.util.json.JsonEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies strict JSON-RPC envelope encoding and decoding.
 */
class TestMcpCodec {
    /**
     * Codec shared by strict-envelope test cases.
     */
    private final McpCodec codec = new McpCodec();

    static Map<String, Object> map(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (int i = 0; i < entries.length; i += 2)
            result.put((String) entries[i], entries[i + 1]);

        return result;
    }

    private String request(String id) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"tools/call\",\"params\":{"
                + "\"name\":\"search\",\"arguments\":{\"q\":\"中文\"},\"_meta\":{"
                + "\"io.modelcontextprotocol/protocolVersion\":\"2026-07-28\","
                + "\"io.modelcontextprotocol/clientCapabilities\":{}}}}";
    }

    @Test
    void modernRequestRoundTripsUtf8AndMetadata() {
        RpcRequest request = (RpcRequest) codec.decode(request("\"search-1\""));
        assertEquals(RequestId.of("search-1"), request.getId());
        assertEquals("tools/call", request.getMethod());
        assertEquals(ProtocolVersion.V_2026_07_28, request.getMetadata().getProtocolVersion());
        assertEquals(request.toMap(), codec.decodeUtf8(codec.encodeUtf8(request)).toMap());
        assertEquals(codec.encode(request), new String(codec.encodeUtf8(request), StandardCharsets.UTF_8));
    }

    @Test
    void exactLargeIntegerAndTextIdsRemainDistinct() {
        String large = "999999999999999999999999999999999999";
        RpcRequest decoded = (RpcRequest) codec.decode(request(large));
        assertEquals(new BigInteger(large), decoded.getId().getValue());
        assertNotEquals(RequestId.of(large), decoded.getId());
        assertEquals(decoded.getId(), ((RpcRequest) codec.decode(codec.encode(decoded))).getId());
        assertEquals(RequestId.of(1), ((RpcRequest) codec.decode(request("1.0"))).getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "true", "[]", "{}", "1.25", "1.000000000000000000001"})
    void rejectsInvalidIdsWithoutTruncation(String id) {
        McpProtocolException error = assertThrows(McpProtocolException.class, () -> codec.decode(request(id)));
        assertEquals(ErrorCodes.INVALID_REQUEST, error.getError().getCode());
        assertNull(error.getError().getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{} {}", "{\"id\":1,\"id\":2}", "{\"x\":NaN}",
            "{\"x\":1,}", "{/*comment*/\"x\":1}", "{'x':1}"})
    void rejectsInvalidJsonAsParseErrors(String json) {
        McpProtocolException error = assertThrows(McpProtocolException.class, () -> codec.decode(json));
        assertEquals(ErrorCodes.PARSE_ERROR, error.getError().getCode());
        assertNotNull(error.getCause());
        assertEquals("Invalid JSON document", error.toErrorResponse().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "[{}]", "42", "{}", "{\"jsonrpc\":\"1.0\",\"method\":\"ping\"}",
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{},\"error\":{}}",
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}",
            "{\"jsonrpc\":\"2.0\",\"result\":{\"resultType\":\"complete\"}}"})
    void rejectsMalformedEnvelopesAndLegacyResults(String json) {
        McpProtocolException error = assertThrows(McpProtocolException.class, () -> codec.decode(json));
        assertEquals(ErrorCodes.INVALID_REQUEST, error.getError().getCode());
    }

    @Test
    void missingAndMalformedMetadataAreInvalidParamsWithOriginalId() {
        String[] params = {"{}", "null", "[]", "{\"_meta\":{}}",
                "{\"_meta\":{\"io.modelcontextprotocol/protocolVersion\":\"2026-07-28\"}}"};

        for (String value : params) {
            McpProtocolException failure = assertThrows(McpProtocolException.class, () -> codec.decode(
                    "{\"jsonrpc\":\"2.0\",\"id\":\"a\",\"method\":\"ping\",\"params\":" + value + "}"));
            assertEquals(ErrorCodes.INVALID_PARAMS, failure.getError().getCode());
            assertEquals(RequestId.of("a"), failure.getError().getId());
        }
    }

    @Test
    void notificationRoundTripAndFailureNeverCreatesRpcReply() {
        RpcNotification notification = new RpcNotification("notifications/progress",
                map("progressToken", "p-1", "progress", 50));
        assertFalse(notification.toMap().containsKey("id"));
        assertInstanceOf(RpcNotification.class, codec.decode(codec.encode(notification)));
        RpcNotification withoutParams = (RpcNotification) codec.decode(
                "{\"jsonrpc\":\"2.0\",\"method\":\"vendor/notice\"}");
        assertFalse(withoutParams.toMap().containsKey("params"));
        McpProtocolException error = assertThrows(McpProtocolException.class, () -> codec.decode(
                "{\"jsonrpc\":\"2.0\",\"method\":\"vendor/notice\",\"params\":null}"));
        assertTrue(error.isNotification());
        assertNull(error.toErrorResponse());
    }

    @Test
    void unknownErrorCodesAndExplicitNullDataSurvive() {
        RpcError error = (RpcError) codec.decode(
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"error\":{\"code\":8123,\"message\":\"vendor error\",\"data\":null}}");
        assertEquals(8123, error.getCode());
        assertTrue(error.hasData());
        assertNull(error.getData());
        assertTrue(codec.encode(error).contains("\"data\":null"));
        RpcError uncorrelated = new RpcError(null, ErrorCodes.PARSE_ERROR, "Invalid JSON");
        assertFalse(codec.decode(codec.encode(uncorrelated)).toMap().containsKey("id"));
        assertFalse(((RpcError) codec.decode(codec.encode(uncorrelated))).hasData());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"-32600\"", "3.14", "2147483648", "null"})
    void errorCodesAreExactIntegers(String code) {
        assertThrows(McpProtocolException.class, () -> codec.decode(
                "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + code + ",\"message\":\"bad\"}}"));
    }

    @Test
    void extensionsRequireExplicitResultTypePolicy() {
        RpcSuccess result = new RpcSuccess(RequestId.of(1), McpResult.ofType("vendor/pending", map("job", "j-1")));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(result));
        McpCodec extensions = new McpCodec(new McpJacksonEngine(), 4096, Collections.singleton("vendor/pending"));
        String json = extensions.encode(result);
        assertEquals("vendor/pending", ((RpcSuccess) extensions.decode(json)).getResult().getResultType());
        assertThrows(McpProtocolException.class, () -> codec.decode(json));
    }

    @Test
    void limitsAndInvalidUtf8AreRejected() {
        McpCodec small = new McpCodec(new McpJacksonEngine(), 8, Collections.emptySet());
        assertThrows(IllegalArgumentException.class, () -> small.decode("\"中文中文\""));
        assertThrows(IllegalArgumentException.class, () -> small.encode(new RpcNotification("test", null)));
        assertThrows(IllegalArgumentException.class, () -> small.decodeUtf8(new byte[9]));
        McpProtocolException failure = assertThrows(McpProtocolException.class,
                () -> codec.decodeUtf8(new byte[]{(byte) 0xc3, (byte) 0x28}));
        assertEquals(ErrorCodes.PARSE_ERROR, failure.getError().getCode());
        String deep = "0";

        for (int i = 0; i < 70; i++)
            deep = "[" + deep + "]";

        final String nested = deep;
        assertThrows(McpProtocolException.class, () -> codec.decode(nested));
    }

    @Test
    void injectedAjEngineIsUsedWithoutDependingOnGlobalJsonUtil() {
        AtomicInteger parses = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        JsonEngine delegate = new McpJacksonEngine();
        JsonEngine tracking = (JsonEngine) Proxy.newProxyInstance(JsonEngine.class.getClassLoader(),
                new Class<?>[]{JsonEngine.class}, (proxy, method, args) -> {
                    if (method.getName().equals("fromJson"))
                        parses.incrementAndGet();
                    if (method.getName().equals("toJson"))
                        writes.incrementAndGet();

                    return method.invoke(delegate, args);
                });
        McpCodec local = new McpCodec(tracking);
        JsonEngine previous = JsonUtil.getEngine();

        try {
            JsonUtil.setEngine((JsonEngine) Proxy.newProxyInstance(JsonEngine.class.getClassLoader(),
                    new Class<?>[]{JsonEngine.class}, (p, m, a) -> {
                        throw new AssertionError("Global engine used");
                    }));
            RpcMessage message = local.decode(request("1"));
            local.encode(message);
            assertEquals(1, parses.get());
            assertEquals(1, writes.get());
        } finally {
            JsonUtil.setEngine(previous);
        }
    }

    @Test
    void structuredJsonValuesDoNotLosePrecisionOrExplicitNull() {
        for (Object value : Arrays.asList(null, true, "text", new BigDecimal("1.00000000000000000001"),
                Arrays.asList("a", "b"), map("nested", false))) {
            RpcSuccess message = new RpcSuccess(RequestId.of("tool-1"),
                    ToolResults.structured(Collections.emptyList(), value, false));
            McpResult result = ((RpcSuccess) codec.decode(codec.encode(message))).getResult();
            assertTrue(result.toMap().containsKey("structuredContent"));
            assertEquals(value, result.toMap().get("structuredContent"));
        }
    }
}
