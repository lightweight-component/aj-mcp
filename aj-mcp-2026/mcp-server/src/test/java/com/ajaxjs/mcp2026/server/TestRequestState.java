package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.core.McpProtocolException;
import com.ajaxjs.mcp2026.core.RequestId;
import com.ajaxjs.mcp2026.core.RpcRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.ajaxjs.mcp2026.server.TestServerRuntime.map;
import static com.ajaxjs.mcp2026.server.TestServerRuntime.request;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies authenticated and expiring multi-round-trip request-state tokens.
 */
class TestRequestState {
    @Test
    void roundTripBindsPrincipalMethodArgumentsAndExpires() {
        byte[] key = RequestState.newKey();
        Clock now = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        RequestState state = new RequestState(key, "service-a", now);
        RpcRequest original = request(1, "tools/call", map("name", "x", "arguments", map("x", 1, "y", "secret")));
        String token = state.seal(original, () -> "alice", map("stage", 2), 1000);
        assertFalse(token.contains("secret"));
        RpcRequest retry = new RpcRequest(RequestId.of(2), original.getMethod(), merged(original.getParams(), "requestState", token));
        assertEquals("2", state.open(retry, () -> "alice").get("stage").toString());
        assertThrows(McpProtocolException.class, () -> state.open(retry, () -> "bob"));
        assertThrows(McpProtocolException.class, () -> new RequestState(key, "service-b", now).open(retry, () -> "alice"));
        assertThrows(McpProtocolException.class, () -> new RequestState(key, "service-a", Clock.offset(now, Duration.ofMillis(1000))).open(retry, () -> "alice"));
        RpcRequest changed = new RpcRequest(RequestId.of(3), retry.getMethod(), merged(retry.getParams(), "arguments", map("x", 2)));
        assertThrows(McpProtocolException.class, () -> state.open(changed, () -> "alice"));
        String[] parts = token.split("\\.");
        byte[] cipher = Base64.getUrlDecoder().decode(parts[2]);
        cipher[0] ^= 1;
        String tampered = parts[0] + "." + parts[1] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(cipher);
        assertThrows(McpProtocolException.class, () -> state.open(new RpcRequest(RequestId.of(4), retry.getMethod(),
                merged(retry.getParams(), "requestState", tampered)), () -> "alice"));
    }

    @Test
    void reorderedPropertiesAreEquivalentButClientMetadataCannotReplaceIdentity() {
        RequestState state = new RequestState(RequestState.newKey(), "test");
        RpcRequest a = request(1, "tools/call", map("name", "x", "arguments", map("a", 1, "b", 2)));
        String token = state.seal(a, null, map("ok", true), 1000);
        RpcRequest b = request(2, "tools/call", map("arguments", map("b", 2, "a", 1), "name", "x", "requestState", token, "inputResponses", map()));
        assertEquals(true, state.open(b, null).get("ok"));
        assertThrows(McpProtocolException.class, () -> state.open(b, () -> "authenticated"));
        assertThrows(IllegalArgumentException.class, () -> state.seal(a, null, map(), 0));
        assertThrows(IllegalArgumentException.class, () -> new RequestState(new byte[8], "test"));
    }

    static Map<String, Object> merged(Map<String, Object> fields, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(fields);
        copy.put(key, value);
        return copy;
    }
}
