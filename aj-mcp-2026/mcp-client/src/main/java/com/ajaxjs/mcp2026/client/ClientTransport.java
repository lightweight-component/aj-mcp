package com.ajaxjs.mcp2026.client;

import com.ajaxjs.mcp2026.core.RequestId;
import com.ajaxjs.mcp2026.core.RpcMessage;
import com.ajaxjs.mcp2026.core.RpcRequest;
import com.ajaxjs.mcp2026.core.ToolDefinition;

import java.io.Closeable;

/**
 * Transport is owned by exactly one client. send() must not block on network I/O.
 */
public interface ClientTransport extends Closeable {
    /**
     * Receives decoded peer messages and terminal transport failures on transport worker threads.
     */
    interface Listener {
        /**
         * Delivers a decoded message. HTTP supplies the originating ID; STDIO supplies null for unscoped notifications.
         *
         * @param scope   request whose response stream produced the message, or {@code null}
         * @param message decoded peer message
         */
        void message(RequestId scope, RpcMessage message);

        /**
         * Reports an I/O, parsing, or stream-lifecycle failure. A null scope means the whole transport has failed.
         *
         * @param scope affected request, or {@code null} for a transport-wide failure
         * @param error local failure; it is not an MCP peer error response
         */
        void failed(RequestId scope, Throwable error);
    }

    /**
     * Starts the receiver side exactly once.
     *
     * @param listener recipient for decoded messages and failures
     */
    void start(Listener listener);

    /**
     * Queues a validated request without blocking on network or pipe I/O.
     *
     * @param request request to send
     */
    void send(RpcRequest request);

    /**
     * Cancels the locally active request identified by {@code id}; implementations may also notify the peer.
     *
     * @param id request identifier to cancel
     */
    void cancel(RequestId id);

    /**
     * Indicates whether the transport applies MCP HTTP metadata and response-stream rules.
     *
     * @return {@code true} for the HTTP transport
     */
    default boolean isHttp() {
        return false;
    }

    /**
     * Registers a discovered tool descriptor for transport-specific processing, such as HTTP parameter headers.
     *
     * @param definition validated tool descriptor
     */
    default void tool(ToolDefinition definition) {
    }

    @Override
    void close();
}
