package com.ajaxjs.mcp2026.core;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-request protocol metadata; clientInfo is descriptive, never authentication.
 * Unknown metadata and capability fields survive round trips.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestMetadata {
    /**
     * Required per-request {@code _meta} key that selects the MCP revision.
     */
    public static final String VERSION = "io.modelcontextprotocol/protocolVersion";

    /**
     * Required per-request {@code _meta} key containing client capabilities.
     */
    public static final String CAPABILITIES = "io.modelcontextprotocol/clientCapabilities";

    /**
     * Optional per-request descriptive client implementation key.
     */
    public static final String CLIENT_INFO = "io.modelcontextprotocol/clientInfo";

    /**
     * Optional result {@code _meta} key identifying the server implementation.
     */
    public static final String SERVER_INFO = "io.modelcontextprotocol/serverInfo";
    /**
     * Immutable complete {@code _meta} object, including unrecognized extension keys.
     */
    private final Map<String, Object> fields;

    /**
     * Creates default per-request metadata for the implemented MCP revision.
     *
     * @param capabilities client capabilities relevant to the request
     * @return validated metadata object
     */
    public static RequestMetadata create(Map<String, Object> capabilities) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(VERSION, ProtocolVersion.V_2026_07_28);
        fields.put(CAPABILITIES, capabilities);

        return from(fields);
    }

    /**
     * Validates and snapshots a request {@code _meta} object.
     *
     * @param value decoded metadata object
     * @return validated immutable metadata
     */
    public static RequestMetadata from(Object value) {
        Map<String, Object> fields = WireValues.object(value, "params._meta");
        WireValues.string(fields.get(VERSION), VERSION);
        WireValues.object(fields.get(CAPABILITIES), CAPABILITIES);

        if (fields.containsKey(CLIENT_INFO)) {
            Map<String, Object> info = WireValues.object(fields.get(CLIENT_INFO), CLIENT_INFO);
            WireValues.string(info.get("name"), "clientInfo.name");
            WireValues.string(info.get("version"), "clientInfo.version");
        }

        return new RequestMetadata(fields);
    }

    /**
     * Returns metadata with one field replaced or added, preserving protocol validation.
     *
     * @param key   metadata key
     * @param value JSON value for the key
     * @return validated metadata copy
     */
    public RequestMetadata with(String key, Object value) {
        Map<String, Object> next = new LinkedHashMap<>(fields);
        next.put(key, value);

        return from(next);
    }

    /**
     * Returns the required protocol version for this request.
     *
     * @return requested MCP protocol version
     */
    public String getProtocolVersion() {
        return (String) fields.get(VERSION);
    }

    /**
     * Returns the required client capability object.
     *
     * @return immutable client capabilities
     */
    public Map<String, Object> getCapabilities() {
        return WireValues.object(fields.get(CAPABILITIES), CAPABILITIES);
    }

    /**
     * Returns the immutable complete {@code _meta} object.
     *
     * @return metadata fields, including unknown extension keys
     */
    public Map<String, Object> toMap() {
        return fields;
    }
}
