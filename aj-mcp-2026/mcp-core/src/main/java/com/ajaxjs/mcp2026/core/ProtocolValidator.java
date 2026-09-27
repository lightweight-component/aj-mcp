package com.ajaxjs.mcp2026.core;

import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Semantic checks separate from decoding: unsupported versions still decode, so
 * the server can return the requested and supported versions to its peer.
 */
@UtilityClass
public class ProtocolValidator {
    /**
     * Rejects deprecated and unallocated MCP-reserved codes before sending an error.
     *
     * @param error error envelope about to be serialized
     * @throws IllegalArgumentException if the code is reserved or forbidden by MCP 2026
     */
    public static void requireOutgoingError(RpcError error) {
        int code = error.getCode();
        if (code == -32002 || (code >= -32099 && code <= -32020
                && code != ErrorCodes.HEADER_MISMATCH && code != ErrorCodes.MISSING_CLIENT_CAPABILITY
                && code != ErrorCodes.UNSUPPORTED_PROTOCOL_VERSION))
            throw new IllegalArgumentException("Error code is reserved or forbidden in MCP 2026");
    }

    /**
     * Rejects a decoded request whose per-request protocol version is not supported.
     *
     * @param request request to validate
     */
    public static void requireSupportedVersion(RpcRequest request) {
        String version = request.getMetadata().getProtocolVersion();

        if (!ProtocolVersion.SUPPORTED.contains(version)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("supported", ProtocolVersion.SUPPORTED);
            data.put("requested", version);

            throw new McpProtocolException(new RpcError(request.getId(), ErrorCodes.UNSUPPORTED_PROTOCOL_VERSION,
                    "Unsupported protocol version", data), false, null);
        }
    }

    /**
     * Checks a required capability subtree, including extensions and subfeatures.
     * Additional advertised capabilities are allowed.
     *
     * @param request  request whose declared capabilities are checked
     * @param required capability subtree required by the operation
     */
    public static void requireCapabilities(RpcRequest request, Map<String, Object> required) {
        Map<String, Object> snapshot = WireValues.object(required, "requiredCapabilities");

        if (!contains(request.getMetadata().getCapabilities(), snapshot)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("requiredCapabilities", snapshot);

            throw new McpProtocolException(new RpcError(request.getId(), ErrorCodes.MISSING_CLIENT_CAPABILITY,
                    "Missing required client capability", data), false, null);
        }
    }

    /**
     * Determines whether every required capability and nested subfeature is advertised.
     *
     * @param actual   capability object supplied in request metadata
     * @param required capability subtree needed by the operation
     * @return {@code true} when {@code actual} contains the complete required subtree
     */
    private static boolean contains(Map<?, ?> actual, Map<?, ?> required) {
        for (Map.Entry<?, ?> entry : required.entrySet()) {
            if (!actual.containsKey(entry.getKey()))
                return false;

            Object value = actual.get(entry.getKey());
            Object expected = entry.getValue();

            if (expected instanceof Map) {
                if (!(value instanceof Map) || !contains((Map<?, ?>) value, (Map<?, ?>) expected))
                    return false;
            } else if (!java.util.Objects.equals(value, expected))
                return false;
        }

        return true;
    }
}
