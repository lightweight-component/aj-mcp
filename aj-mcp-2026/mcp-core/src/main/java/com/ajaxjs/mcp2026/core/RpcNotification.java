package com.ajaxjs.mcp2026.core;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

/**
 * A notification cannot carry an ID. Params may be absent, but not JSON null.
 */
@Getter
public final class RpcNotification implements RpcMessage {
    /**
     * Name of the one-way JSON-RPC method.
     */
    private final String method;

    /**
     * Immutable parameter snapshot, or {@code null} when the params member is absent.
     */
    private final Map<String, Object> params;

    /**
     * Creates a one-way JSON-RPC notification.
     *
     * @param method notification method name
     * @param params optional object parameters; {@code null} omits the member
     */
    public RpcNotification(String method, Map<String, Object> params) {
        this.method = Objects.requireNonNull(method, "method");
        this.params = params == null ? null : WireValues.object(params, "params");
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> value = WireValues.envelope();
        value.put("method", method);

        if (params != null)
            value.put("params", params);

        return value;
    }
}
