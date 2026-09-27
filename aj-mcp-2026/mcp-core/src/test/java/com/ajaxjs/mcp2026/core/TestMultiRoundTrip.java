package com.ajaxjs.mcp2026.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.ajaxjs.mcp2026.core.TestMcpCodec.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies multi-round-trip retry construction and parameter validation.
 */
class TestMultiRoundTrip {
    private RpcRequest request() {
        return RpcRequest.create(RequestId.of(1), "tools/call", map("name", "example"), RequestMetadata.create(map()));
    }

    @Test
    void retryPreservesOpaqueStateAndArguments() {
        RpcRequest original = request();
        String state = " opaque\u0000密文 ";
        Map<String, Object> inputs = map("question", map("action", "accept"));
        RpcSuccess response = new RpcSuccess(original.getId(), McpResult.inputRequired(
                map("question", map("method", "elicitation/create")), state));
        RpcRequest retry = MultiRoundTrip.retry(original, response, RequestId.of(2), inputs);
        inputs.clear();
        assertEquals(state, retry.getParams().get("requestState"));
        assertEquals("example", retry.getParams().get("name"));
        assertEquals(original.getMetadata().toMap(), retry.getMetadata().toMap());
        assertFalse(original.getParams().containsKey("requestState"));
        McpCodec codec = new McpCodec();
        assertEquals(retry.toMap(), codec.decode(codec.encode(retry)).toMap());
    }

    @Test
    void latestRoundRemovesStaleStateAndResponses() {
        RpcRequest original = request();
        RpcRequest first = MultiRoundTrip.retry(original,
                new RpcSuccess(original.getId(), McpResult.inputRequired(null, "old")), RequestId.of(2), map("old", map()));
        RpcRequest second = MultiRoundTrip.retry(first,
                new RpcSuccess(first.getId(), McpResult.inputRequired(map(), null)), RequestId.of(3), null);
        assertFalse(second.getParams().containsKey("requestState"));
        assertEquals(map(), second.getParams().get("inputResponses"));
    }

    @Test
    void rejectsWrongCorrelationSameIdAndTerminalResults() {
        RpcRequest original = request();
        RpcSuccess input = new RpcSuccess(original.getId(), McpResult.inputRequired(null, "state"));
        assertThrows(IllegalArgumentException.class, () -> MultiRoundTrip.retry(original, input, original.getId(), null));
        assertThrows(IllegalArgumentException.class, () -> MultiRoundTrip.retry(original,
                new RpcSuccess(RequestId.of(9), input.getResult()), RequestId.of(2), null));
        assertThrows(IllegalArgumentException.class, () -> MultiRoundTrip.retry(original,
                new RpcSuccess(original.getId(), McpResult.empty()), RequestId.of(2), null));
        RpcRequest discovery = Discovery.request(original.getId(), original.getMetadata());
        assertThrows(IllegalArgumentException.class, () -> MultiRoundTrip.retry(discovery, input, RequestId.of(2), null));
    }

    @Test
    void clientRequiresAllInputsButServerAcceptsPartialObjects() {
        RpcRequest original = request();
        RpcSuccess input = new RpcSuccess(original.getId(), McpResult.inputRequired(
                map("a", map("method", "roots/list")), null));
        assertThrows(IllegalArgumentException.class, () -> MultiRoundTrip.retry(original, input, RequestId.of(2), map()));
        assertDoesNotThrow(() -> RpcRequest.create(RequestId.of(2), "tools/call",
                map("inputResponses", map()), original.getMetadata()));
    }

    @Test
    void invalidContinuationFieldsBecomeInvalidParamsOnDecode() {
        McpCodec codec = new McpCodec();
        for (Map<String, Object> fields : java.util.Arrays.asList(map("requestState", 1),
                map("inputResponses", null), map("inputResponses", map("a", false)))) {
            fields.put("_meta", request().getMetadata().toMap());
            String json = new com.ajaxjs.mcp2026.core.json.McpJacksonEngine().toJson(
                    map("jsonrpc", "2.0", "id", 1, "method", "tools/call", "params", fields));
            McpProtocolException error = assertThrows(McpProtocolException.class, () -> codec.decode(json));
            assertEquals(ErrorCodes.INVALID_PARAMS, error.getError().getCode());
            assertEquals(RequestId.of(1), error.getError().getId());
        }
    }
}
