package com.ajaxjs.mcp2026.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ajaxjs.mcp2026.core.TestMcpCodec.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies immutable protocol model construction and wire representations.
 */
class TestProtocolModels {
    /**
     * Codec used to verify each model's canonical wire representation.
     */
    private final McpCodec codec = new McpCodec();

    @Test
    void metadataAndPayloadSnapshotsAreDeeplyImmutable() {
        Map<String, Object> capabilities = map("elicitation", map("form", map()));
        RequestMetadata meta = RequestMetadata.create(capabilities)
                .with("vendor.example/trace", map("tag", "v1"))
                .with(RequestMetadata.CLIENT_INFO, map("name", "demo", "version", "1"));
        List<Object> values = new ArrayList<>();
        values.add("initial");
        RpcRequest request = RpcRequest.create(RequestId.of(7), "tools/call", map("arguments", map("values", values)), meta);
        capabilities.clear();
        values.add("later");
        assertTrue(meta.getCapabilities().containsKey("elicitation"));
        assertFalse(codec.encode(request).contains("later"));
        assertThrows(UnsupportedOperationException.class, () -> request.getParams().put("bad", true));
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.getParams().get("arguments");
        @SuppressWarnings("unchecked")
        List<Object> captured = (List<Object>) arguments.get("values");
        assertThrows(UnsupportedOperationException.class, () -> captured.add("bad"));
        assertEquals(meta.toMap(), ((RpcRequest) codec.decode(codec.encode(request))).getMetadata().toMap());
    }

    @Test
    void nullCapabilitiesAndMalformedClientIdentityAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RequestMetadata.create(null));
        assertThrows(IllegalArgumentException.class, () -> RequestMetadata.create(map())
                .with(RequestMetadata.CLIENT_INFO, map("name", "demo")));
        assertThrows(IllegalArgumentException.class, () -> RpcRequest.create(RequestId.of(1), "ping",
                map("_meta", map()), RequestMetadata.create(map())));
    }

    @Test
    void supportedVersionValidationProducesNegotiationData() {
        RpcRequest modern = RpcRequest.create(RequestId.of(1), "server/discover", map(), RequestMetadata.create(map()));
        assertDoesNotThrow(() -> ProtocolValidator.requireSupportedVersion(modern));
        RpcRequest other = RpcRequest.create(RequestId.of(2), "server/discover", map(),
                modern.getMetadata().with(RequestMetadata.VERSION, "2099-01-01"));
        assertInstanceOf(RpcRequest.class, codec.decode(codec.encode(other)));
        McpProtocolException failure = assertThrows(McpProtocolException.class,
                () -> ProtocolValidator.requireSupportedVersion(other));
        assertEquals(ErrorCodes.UNSUPPORTED_PROTOCOL_VERSION, failure.getError().getCode());
        assertEquals(map("supported", ProtocolVersion.SUPPORTED, "requested", "2099-01-01"), failure.getError().getData());
    }

    @Test
    void capabilityChecksArePerRequestAndIncludeNestedFeatures() {
        Map<String, Object> required = map("elicitation", map("form", map()));
        RpcRequest permitted = RpcRequest.create(RequestId.of(1), "tools/call", map(),
                RequestMetadata.create(map("elicitation", map("form", map(), "url", map()))));
        RpcRequest denied = RpcRequest.create(RequestId.of(1), "tools/call", map(),
                RequestMetadata.create(map("elicitation", map("url", map()))));
        assertDoesNotThrow(() -> ProtocolValidator.requireCapabilities(permitted, required));
        McpProtocolException failure = assertThrows(McpProtocolException.class,
                () -> ProtocolValidator.requireCapabilities(denied, required));
        assertEquals(ErrorCodes.MISSING_CLIENT_CAPABILITY, failure.getError().getCode());
        assertEquals(map("requiredCapabilities", required), failure.getError().getData());
    }

    @Test
    void inputRequiredSupportsOpaqueStateAndRoundTripInputMap() {
        Map<String, Object> inputs = map("confirm", map("method", "elicitation/create",
                "params", map("message", "Proceed?", "requestedSchema", map("type", "object"))));
        McpResult result = McpResult.inputRequired(inputs, "opaque-state");
        RpcSuccess response = new RpcSuccess(RequestId.of("r"), result);
        assertEquals(response.toMap(), codec.decode(codec.encode(response)).toMap());
        assertEquals("opaque-only", McpResult.inputRequired(null, "opaque-only").toMap().get("requestState"));
        assertThrows(IllegalArgumentException.class, () -> McpResult.inputRequired(null, null));
        assertThrows(IllegalArgumentException.class, () -> McpResult.from(map("resultType", "input_required", "requestState", map())));
        RpcRequest retry = RpcRequest.create(RequestId.of("retry"), "tools/call",
                map("name", "delete", "inputResponses", map("confirm", map("action", "accept")), "requestState", "opaque-state"),
                RequestMetadata.create(map()));
        assertEquals(retry.toMap(), codec.decode(codec.encode(retry)).toMap());
    }

    @Test
    void businessErrorsRemainCompletedToolResults() {
        McpResult result = ToolResults.error("Input is not valid for this operation");
        RpcMessage response = codec.decode(codec.encode(new RpcSuccess(RequestId.of(1), result)));
        assertInstanceOf(RpcSuccess.class, response);
        assertEquals(McpResult.COMPLETE, ((RpcSuccess) response).getResult().getResultType());
        assertEquals(true, ((RpcSuccess) response).getResult().toMap().get("isError"));
    }

    @Test
    void fullToolSchemasAndExtensionFieldsSurviveSerialization() {
        Map<String, Object> schema = map("type", "object", "$defs", map("item", map("type", "string")),
                "properties", map("items", map("type", "array", "items", map("$ref", "#/$defs/item"))),
                "required", Collections.singletonList("items"), "additionalProperties", false);
        ToolDefinition tool = ToolDefinition.create("search", schema)
                .with("outputSchema", map("oneOf", java.util.Arrays.asList(map("type", "string"), map("type", "null"))))
                .with("_meta", map("vendor.example/key", "value"));
        schema.clear();
        RpcSuccess response = new RpcSuccess(RequestId.of(1), McpResult.complete(map("tools", Collections.singletonList(tool.toMap()),
                "ttlMs", 0, "cacheScope", "private")));
        String json = codec.encode(response);
        assertTrue(json.contains("$defs"));
        assertTrue(json.contains("oneOf"));
        assertTrue(json.contains("vendor.example/key"));
        RpcSuccess decoded = (RpcSuccess) codec.decode(json);
        assertEquals(json, codec.encode(decoded));
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.create("bad", null));
        assertThrows(IllegalArgumentException.class, () -> ToolDefinition.create("bad", map("type", "string")));
    }

    @Test
    void rejectsNonJsonObjectsCyclesAndNonFiniteNumbers() {
        assertThrows(IllegalArgumentException.class, () -> McpResult.complete(map("bad", new Object())));
        assertThrows(IllegalArgumentException.class, () -> McpResult.complete(map("bad", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> McpResult.complete(map("bad", Double.POSITIVE_INFINITY)));
        Map<String, Object> cycle = map();
        cycle.put("cycle", cycle);
        assertThrows(IllegalArgumentException.class, () -> McpResult.complete(cycle));
    }
}
