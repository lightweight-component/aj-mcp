package com.ajaxjs.mcp2026.core;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire helpers for server discovery. No connection, cache, or negotiation state
 * is retained here; callers must isolate private caches by authorization context.
 */
public final class Discovery {
    /**
     * MCP method name used to retrieve server identity and capabilities.
     */
    public static final String METHOD = "server/discover";

    /**
     * Prevents instantiation of this stateless discovery-message utility.
     */
    private Discovery() {
    }

    /**
     * Creates an ordinary per-request discovery message, not a handshake.
     *
     * @param id       request identifier chosen by the caller
     * @param metadata protocol version and client capability metadata for this request
     * @return validated discovery request ready for transmission
     */
    public static RpcRequest request(RequestId id, RequestMetadata metadata) {
        return RpcRequest.create(id, METHOD, Collections.emptyMap(), metadata);
    }

    /**
     * Builds a discovery result. Zero TTL disables freshness; private scope is
     * the conservative default. The implementation identity is descriptive only.
     *
     * @param capabilities server capability object advertised for this response
     * @param name         descriptive server implementation name
     * @param version      descriptive server implementation version
     * @return validated complete discovery result with conservative cache metadata
     */
    public static McpResult result(Map<String, Object> capabilities, String name, String version) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", name);
        info.put("version", version);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("supportedVersions", ProtocolVersion.SUPPORTED);
        fields.put("capabilities", capabilities);
        fields.put("_meta", Collections.singletonMap(RequestMetadata.SERVER_INFO, info));
        fields.put("ttlMs", 0);
        fields.put("cacheScope", "private");

        return validate(McpResult.complete(fields));
    }

    /**
     * Checks discovery-specific fields after generic decoding, preserving unknown
     * extensions. This does not implement a general JSON Schema validator.
     *
     * @param result generic decoded result to validate as discovery data
     * @return the original result after successful validation
     * @throws IllegalArgumentException if the result is not a valid discovery response
     */
    public static McpResult validate(McpResult result) {
        if (!McpResult.COMPLETE.equals(result.getResultType()))
            throw new IllegalArgumentException("Discovery requires a complete result");

        Map<String, Object> fields = result.toMap();
        Object versions = fields.get("supportedVersions");

        if (!(versions instanceof List))
            throw new IllegalArgumentException("supportedVersions must be an array");

        for (Object version : (List<?>) versions)
            WireValues.string(version, "supportedVersions[]");

        WireValues.object(fields.get("capabilities"), "capabilities");

        if (fields.containsKey("instructions"))
            WireValues.string(fields.get("instructions"), "instructions");
        Object ttl = fields.get("ttlMs");

        if (!(ttl instanceof Number) || new BigDecimal(ttl.toString()).signum() < 0
                || new BigDecimal(ttl.toString()).stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("ttlMs must be a non-negative integer");
        Object scope = fields.get("cacheScope");

        if (!"public".equals(scope) && !"private".equals(scope))
            throw new IllegalArgumentException("cacheScope must be public or private");

        if (fields.containsKey("_meta")) {
            Map<String, Object> meta = WireValues.object(fields.get("_meta"), "_meta");
            if (meta.containsKey(RequestMetadata.SERVER_INFO)) {
                Map<String, Object> info = WireValues.object(meta.get(RequestMetadata.SERVER_INFO), "serverInfo");
                WireValues.string(info.get("name"), "serverInfo.name");
                WireValues.string(info.get("version"), "serverInfo.version");
            }
        }

        return result;
    }
}
