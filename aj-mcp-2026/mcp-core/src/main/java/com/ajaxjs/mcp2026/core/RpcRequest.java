package com.ajaxjs.mcp2026.core;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A modern request always carries its own metadata in params._meta.
 */
@Getter
public final class RpcRequest implements RpcMessage {
    /**
     * Non-null JSON-RPC identifier used to correlate the response.
     */
    private final RequestId id;

    /**
     * Name of the requested MCP method.
     */
    private final String method;

    /**
     * Immutable params snapshot, including the required {@code _meta} object.
     */
    private final Map<String, Object> params;
    /**
     * Per-request protocol version and client capabilities extracted from {@code params._meta}.
     */
    private final RequestMetadata metadata;

    /**
     * Creates a request from a complete parameter object containing required metadata.
     *
     * @param id     non-null JSON-RPC request identifier
     * @param method MCP method name
     * @param params object parameters containing {@code _meta}
     */
    public RpcRequest(RequestId id, String method, Map<String, Object> params) {
        this.id = Objects.requireNonNull(id, "id");
        this.method = Objects.requireNonNull(method, "method");
        this.params = WireValues.object(params, "params");
        this.metadata = RequestMetadata.from(this.params.get("_meta"));
        if (Subscriptions.LISTEN.equals(method))
            Subscriptions.filter(this.params.get("notifications"));
        if (MultiRoundTrip.supports(method))
            MultiRoundTrip.validateParams(this.params);
    }

    /**
     * Creates a request while preventing method arguments from replacing required metadata.
     *
     * @param id        non-null JSON-RPC request identifier
     * @param method    MCP method name
     * @param arguments method-specific parameters excluding {@code _meta}
     * @param metadata  required per-request version and client capabilities
     * @return validated request
     */
    public static RpcRequest create(RequestId id, String method, Map<String, Object> arguments, RequestMetadata metadata) {
        Map<String, Object> params = new LinkedHashMap<>(arguments);

        if (params.containsKey("_meta"))
            throw new IllegalArgumentException("Supply metadata through the metadata argument");

        params.put("_meta", metadata.toMap());

        return new RpcRequest(id, method, params);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> value = WireValues.envelope();
        value.put("id", id.getValue());
        value.put("method", method);
        value.put("params", params);

        return value;
    }
}
