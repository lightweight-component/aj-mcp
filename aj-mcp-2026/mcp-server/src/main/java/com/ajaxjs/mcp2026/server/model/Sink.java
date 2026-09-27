package com.ajaxjs.mcp2026.server.model;

import com.ajaxjs.mcp2026.core.RpcMessage;

import java.io.IOException;

/**
 * Serialized outbound transport boundary for one MCP connection.
 */
@FunctionalInterface
public interface Sink {
    /**
     * Writes one already-validated protocol message to the peer.
     *
     * @param message response or notification to serialize
     * @throws IOException if the underlying peer stream can no longer accept output
     */
    void send(RpcMessage message) throws IOException;
}
