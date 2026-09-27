package com.ajaxjs.mcp2026.client.model;

import com.ajaxjs.mcp2026.core.RpcRequest;
import com.ajaxjs.mcp2026.core.RpcSuccess;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

/**
 * Per-request client state shared by response dispatch, cancellation, and deadline tasks.
 * The owning client removes this record once {@link #future} reaches a terminal state.
 */
@Getter
@RequiredArgsConstructor
public final class Pending {
    /**
     * Outbound request whose JSON-RPC identifier owns this correlation state.
     */
    final RpcRequest request;

    /**
     * Subscription lifecycle state, or {@code null} when this is an ordinary request.
     */
    final Subscription subscription;

    /**
     * Future completed with the correlated success response or a local/remote failure.
     */
    final CompletableFuture<RpcSuccess> future = new CompletableFuture<>();

    /**
     * Scheduled finite deadline; cleared by completion so it cannot cancel a completed request.
     */
    @Setter
    volatile ScheduledFuture<?> deadline;

    /**
     * Guard set while holding this record's monitor immediately before handing the request to the transport.
     * Completion uses it to decide whether cancellation must be propagated to the peer.
     */
    @Setter
    boolean sent;
}
