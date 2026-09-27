package com.ajaxjs.mcp2026.core;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Protocol error response. A null Java ID means an uncorrelated error and is
 * omitted on the modern wire. Explicit JSON null error data is preserved.
 */
public final class RpcError implements RpcMessage {
    /**
     * Correlated request identifier, or {@code null} when the malformed request has no usable ID.
     */
    @Getter
    private final RequestId id;
    /**
     * Standard or extension JSON-RPC error code.
     */
    @Getter
    private final int code;
    /**
     * Peer-safe error summary; parser causes remain outside the wire object.
     */
    @Getter
    private final String message;
    /**
     * Distinguishes an absent {@code data} member from an explicitly null one.
     */
    private final boolean hasData;
    /**
     * Immutable optional JSON error data, possibly explicit {@code null}.
     */
    @Getter
    private final Object data;

    /**
     * Creates an error response with no {@code data} member.
     *
     * @param id      request identifier, or {@code null} when it could not be recovered
     * @param code    JSON-RPC integer error code
     * @param message peer-safe error message
     */
    public RpcError(RequestId id, int code, String message) {
        this(id, code, message, false, null);
    }

    /**
     * Creates an error response whose {@code data} member is present, including explicit null.
     *
     * @param id      request identifier, or {@code null} when it could not be recovered
     * @param code    JSON-RPC integer error code
     * @param message peer-safe error message
     * @param data    JSON error data
     */
    public RpcError(RequestId id, int code, String message, Object data) {
        this(id, code, message, true, data);
    }

    /**
     * Creates an error while retaining whether the optional data member was present.
     *
     * @param id      correlated request identifier, if available
     * @param code    JSON-RPC integer error code
     * @param message peer-safe error summary
     * @param hasData whether the wire error contains {@code data}
     * @param data    optional JSON data
     */
    private RpcError(RequestId id, int code, String message, boolean hasData, Object data) {
        this.id = id;
        this.code = code;
        this.message = Objects.requireNonNull(message, "message");
        this.hasData = hasData;
        this.data = WireValues.copy(data, 0);
    }


    /**
     * Determines whether the optional wire {@code data} member is present.
     *
     * @return {@code true} when the error contains {@code data}, even if it is null
     */
    public boolean hasData() {
        return hasData;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> value = WireValues.envelope();

        if (id != null)
            value.put("id", id.getValue());

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);

        if (hasData)
            error.put("data", data);

        value.put("error", error);

        return value;
    }
}
