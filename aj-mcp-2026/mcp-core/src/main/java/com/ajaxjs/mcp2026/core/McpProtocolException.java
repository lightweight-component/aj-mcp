package com.ajaxjs.mcp2026.core;

import lombok.Getter;

/**
 * Decode/validation failure, not a received remote error. A failed notification
 * remains response-free; callers must not serialize a null error response.
 */
@Getter
public final class McpProtocolException extends RuntimeException {
    /**
     * Serialization compatibility identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Sanitized JSON-RPC error suitable for a correlated request response.
     */
    private final RpcError error;

    /**
     * Whether the invalid inbound envelope was a notification and therefore cannot be answered.
     */
    private final boolean notification;

    /**
     * Creates a local decoded or validation failure with a sanitized protocol error.
     *
     * @param error        response-safe JSON-RPC error
     * @param notification whether the invalid message must remain response-free
     * @param cause        local diagnostic that must not be sent to the peer
     */
    public McpProtocolException(RpcError error, boolean notification, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
        this.notification = notification;
    }

    /**
     * Returns the error response permitted by JSON-RPC notification semantics.
     *
     * @return the sanitized error, or {@code null} for a notification
     */
    public RpcError toErrorResponse() {
        return notification ? null : error;
    }
}
