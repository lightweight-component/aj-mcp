package com.ajaxjs.mcp2026.client;

import com.ajaxjs.mcp2026.core.RpcError;
import lombok.Getter;

/**
 * Peer error, distinct from a local protocol-validation failure.
 */
public final class McpRemoteException extends RuntimeException {
    /**
     * Complete error envelope received from the peer, including its optional safe data.
     */
    @Getter
    private final RpcError error;

    /**
     * Wraps an error response so asynchronous calls fail exceptionally rather than return a success result.
     *
     * @param error error response received from the MCP peer
     */
    public McpRemoteException(RpcError error) {
        super(error.getMessage());
        this.error = error;
    }
}
