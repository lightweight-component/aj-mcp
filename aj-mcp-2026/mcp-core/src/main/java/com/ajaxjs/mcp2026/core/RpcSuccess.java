package com.ajaxjs.mcp2026.core;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;

/**
 * A result response, separate from protocol errors and notifications.
 */
@Getter
public final class RpcSuccess implements RpcMessage {
    /**
     * Identifier copied from the request that this result completes.
     */
    private final RequestId id;

    /**
     * Result object, which must carry its MCP 2026 {@code resultType}.
     */
    private final McpResult result;

    /**
     * Creates a successful response correlated to a request.
     *
     * @param id     identifier copied from the completed request
     * @param result typed MCP result payload
     */
    public RpcSuccess(RequestId id, McpResult result) {
        this.id = Objects.requireNonNull(id, "id");
        this.result = Objects.requireNonNull(result, "result");
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> value = WireValues.envelope();
        value.put("id", id.getValue());
        value.put("result", result.toMap());

        return value;
    }
}
