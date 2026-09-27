package com.ajaxjs.mcp2026.core;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Extensible result payload. Method-specific schemas are validated separately.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpResult {
    /**
     * Result type indicating that processing has finished.
     */
    public static final String COMPLETE = "complete";

    /**
     * Result type indicating that the caller must supply additional MRTR input.
     */
    public static final String INPUT_REQUIRED = "input_required";

    /**
     * Immutable result object, preserving core and negotiated extension fields.
     */
    private final Map<String, Object> fields;

    /**
     * Validates and snapshots a complete MCP result object.
     *
     * @param fields result fields including the required {@code resultType}
     * @return immutable result snapshot
     */
    public static McpResult from(Map<String, Object> fields) {
        Map<String, Object> snapshot = WireValues.object(fields, "result");
        String type = WireValues.string(snapshot.get("resultType"), "result.resultType");

        if (snapshot.containsKey("_meta"))
            WireValues.object(snapshot.get("_meta"), "result._meta");

        if (INPUT_REQUIRED.equals(type)) {
            if (!snapshot.containsKey("inputRequests") && !snapshot.containsKey("requestState"))
                throw new IllegalArgumentException("input_required needs inputRequests or requestState");

            if (snapshot.containsKey("requestState"))
                WireValues.string(snapshot.get("requestState"), "requestState");

            if (snapshot.containsKey("inputRequests")) {
                Map<String, Object> inputs = WireValues.object(snapshot.get("inputRequests"), "inputRequests");

                for (Object input : inputs.values()) {
                    Map<String, Object> request = WireValues.object(input, "inputRequest");
                    WireValues.string(request.get("method"), "inputRequest.method");

                    if (request.containsKey("params"))
                        WireValues.object(request.get("params"), "inputRequest.params");
                }
            }
        }

        return new McpResult(snapshot);
    }

    /**
     * Creates a terminal {@code complete} result.
     *
     * @param fields method-specific result fields
     * @return complete result
     */
    public static McpResult complete(Map<String, Object> fields) {
        return ofType(COMPLETE, fields);
    }

    /**
     * Creates a result of a core or negotiated extension type.
     *
     * @param type   result type to add to the supplied fields
     * @param fields fields other than {@code resultType}
     * @return validated immutable result
     */
    public static McpResult ofType(String type, Map<String, Object> fields) {
        Map<String, Object> result = new LinkedHashMap<>(fields);

        if (result.containsKey("resultType"))
            throw new IllegalArgumentException("resultType is supplied separately");

        result.put("resultType", type);

        return from(result);
    }

    /**
     * Creates an MRTR result requesting additional client input or retaining opaque request state.
     *
     * @param inputRequests client requests keyed by implementation-defined identifiers, if any
     * @param requestState  opaque state to return with a later input response, if any
     * @return {@code input_required} result
     */
    public static McpResult inputRequired(Map<String, Object> inputRequests, String requestState) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (inputRequests != null)
            result.put("inputRequests", inputRequests);

        if (requestState != null)
            result.put("requestState", requestState);

        return ofType(INPUT_REQUIRED, result);
    }

    /**
     * Creates a terminal result without method-specific fields.
     *
     * @return empty {@code complete} result
     */
    public static McpResult empty() {
        return complete(Collections.emptyMap());
    }

    /**
     * Returns the MCP result discriminator.
     *
     * @return required {@code resultType} string
     */
    public String getResultType() {
        return (String) fields.get("resultType");
    }

    /**
     * Returns the immutable complete result object.
     *
     * @return result fields including {@code resultType}
     */
    public Map<String, Object> toMap() {
        return fields;
    }
}
