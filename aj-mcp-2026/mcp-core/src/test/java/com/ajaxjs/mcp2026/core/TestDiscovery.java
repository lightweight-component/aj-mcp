package com.ajaxjs.mcp2026.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.ajaxjs.mcp2026.core.TestMcpCodec.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies stateless server-discovery request and result validation.
 */
class TestDiscovery {
    @Test
    void requestRoundTripsWithoutSessionState() {
        McpCodec codec = new McpCodec();
        RpcRequest request = Discovery.request(RequestId.of(1), RequestMetadata.create(map()));
        RpcRequest decoded = (RpcRequest) codec.decode(codec.encode(request));
        assertEquals(Discovery.METHOD, decoded.getMethod());
        assertEquals(request.getParams(), decoded.getParams());
    }

    @Test
    void resultRoundTripsWithConservativeCacheDefaults() {
        McpCodec codec = new McpCodec();
        Map<String, Object> capabilities = map("tools", map());
        McpResult result = Discovery.result(capabilities, "example", "1");
        capabilities.clear();
        RpcSuccess response = (RpcSuccess) codec.decode(codec.encode(new RpcSuccess(RequestId.of(1), result)));
        assertSame(response.getResult(), Discovery.validate(response.getResult()));
        assertEquals("private", result.toMap().get("cacheScope"));
        assertEquals(0, result.toMap().get("ttlMs"));
        assertEquals(map("tools", map()), result.toMap().get("capabilities"));
    }

    @Test
    void requiredDiscoveryFieldsCannotBeOmitted() {
        for (String key : Arrays.asList("supportedVersions", "capabilities", "ttlMs", "cacheScope")) {
            Map<String, Object> fields = valid();
            fields.remove(key);
            assertThrows(IllegalArgumentException.class, () -> Discovery.validate(McpResult.complete(fields)), key);
        }
    }

    @Test
    void malformedFieldsAreRejected() {
        Map<String, Object> invalid = map("supportedVersions", Arrays.asList(1), "capabilities", false,
                "ttlMs", -1, "cacheScope", "shared", "instructions", 1,
                "_meta", map(RequestMetadata.SERVER_INFO, map("name", "missing-version")));
        invalid.forEach((key, value) -> {
            Map<String, Object> fields = valid();
            fields.put(key, value);
            assertThrows(IllegalArgumentException.class, () -> Discovery.validate(McpResult.complete(fields)), key);
        });
        assertThrows(IllegalArgumentException.class,
                () -> Discovery.validate(McpResult.inputRequired(null, "opaque")));
    }

    @Test
    void extensionsAndUnknownVersionsArePreserved() {
        Map<String, Object> fields = valid();
        fields.put("supportedVersions", Arrays.asList("2026-07-28", "future"));
        fields.put("vendor.example/extension", map("nested", Collections.singletonList(true)));
        fields.put("ttlMs", 1.0);
        fields.put("cacheScope", "public");
        McpResult result = Discovery.validate(McpResult.complete(fields));
        assertEquals(fields.get("vendor.example/extension"), result.toMap().get("vendor.example/extension"));
        assertThrows(UnsupportedOperationException.class, () -> result.toMap().clear());
    }

    private static Map<String, Object> valid() {
        return map("supportedVersions", ProtocolVersion.SUPPORTED, "capabilities", map(),
                "ttlMs", 0, "cacheScope", "private");
    }
}
