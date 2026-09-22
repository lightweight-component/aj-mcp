package com.ajaxjs.mcp.server.model;

/**
 * Lifecycle state for a transport session during MCP initialization.
 * <p>
 * Transports use this enum to reject ordinary requests until the peer has completed the
 * required {@code initialize} and {@code initialized} handshake.
 */
public enum SessionState {
    /**
     * Session id exists, but no valid {@code initialize} request has been accepted yet.
     */
    NEW,

    /**
     * {@code initialize} succeeded and the server is waiting for the client {@code initialized} notification.
     */
    INITIALIZING,

    /**
     * Handshake is complete and ordinary feature requests may be processed.
     */
    READY
}