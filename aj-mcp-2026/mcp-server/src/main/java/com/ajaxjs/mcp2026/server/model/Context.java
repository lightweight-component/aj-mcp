package com.ajaxjs.mcp2026.server.model;

import com.ajaxjs.mcp2026.core.*;
import lombok.Getter;
import lombok.Setter;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-request execution state exposed to a server handler.
 * It coordinates request cancellation, terminal completion, and protocol-validated notifications without
 * allowing one connection's state to leak into another.
 */
@Getter
public class Context {
    /**
     * Connection responsible for serialized output and connection-wide cancellation.
     */
    private final Connection connection;
    /**
     * Immutable request associated with this handler invocation.
     */
    private final RpcRequest request;
    /**
     * Completion signal used by connection cleanup and request lifetime management.
     */
    private final CompletableFuture<Void> done = new CompletableFuture<>();
    /**
     * Worker task for ordinary dispatch, interrupted when the request is cancelled.
     */
    @Setter
    private volatile Future<?> task;

    /**
     * Scheduled finite deadline for a request or subscription.
     */
    @Setter
    private volatile ScheduledFuture<?> alarm;
    /**
     * Atomic terminal-output guard.
     */
    private final AtomicBoolean ended = new AtomicBoolean();
    /**
     * Explicit cancellation marker distinct from normal terminal completion.
     */
    private volatile boolean cancelled;

    /**
     * Accepted subscription acknowledgement constraining follow-up notifications.
     */
    @Setter
    private RpcNotification ack;
    /**
     * Most recently emitted progress, enforcing a strictly increasing sequence.
     */
    private double lastProgress = Double.NEGATIVE_INFINITY;

    /**
     * Validator used for request-scoped notification and subscription checks.
     */
    final ProtocolSchema protocol;

    /**
     * Creates context for one already-correlated inbound request.
     *
     * @param connection owning connection
     * @param request    inbound request
     * @param protocol   shared protocol validator
     */
    public Context(Connection connection, RpcRequest request, ProtocolSchema protocol) {
        this.connection = connection;
        this.request = request;
        this.protocol = protocol;
    }

    /**
     * @return immutable request being handled
     */
    public RpcRequest request() {
        return request;
    }

    /**
     * @return authenticated transport principal, or {@code null} for anonymous connections
     */
    public Principal principal() {
        return connection.getPrincipal();
    }

    /**
     * @return whether the request ended or its connection closed
     */
    public boolean isCancelled() {
        return ended.get() || connection.getStopped().get();
    }

    /**
     * True only when closure canceled this context, not after a normal response.
     *
     * @return {@code true} when cancellation or connection closure suppresses further output
     */
    public boolean isExplicitlyCancelled() {
        return cancelled || connection.getStopped().get();
    }

    /**
     * Stops cooperative handler work after cancellation while preserving Java interruption semantics.
     *
     * @throws InterruptedException if the request or worker thread has been canceled
     */
    public void checkCancelled() throws InterruptedException {
        if (isCancelled() || Thread.currentThread().isInterrupted())
            throw new InterruptedException("Request cancelled");
    }

    /**
     * Validates and emits a request-scoped notification while preserving output order with terminal completion.
     *
     * @param notification notification permitted by the request or subscription acknowledgement
     */
    public synchronized void notify(RpcNotification notification) {
        if (ended.get())
            return;

        if (Subscriptions.LISTEN.equals(request.getMethod()))
            protocol.requireSubscriptionNotification(request, ack, notification);
        else
            protocol.requireRequestNotification(request, notification);

        if (connection.isHttp() && "notifications/cancelled".equals(notification.getMethod()))
            throw new IllegalArgumentException("HTTP uses stream cancellation");

        emit(notification);
    }

    /**
     * Emits a strictly increasing progress notification when the request opted in with a progress token.
     *
     * @param progress current finite progress value
     * @param total    optional finite upper bound
     * @param message  optional human-readable status
     */
    public synchronized void progress(double progress, Double total, String message) {
        Object token = request.getMetadata().toMap().get("progressToken");

        if (token == null)
            return;

        if (!Double.isFinite(progress) || progress <= lastProgress || total != null && (!Double.isFinite(total) || progress > total))
            throw new IllegalArgumentException("Progress must increase and not exceed total");

        lastProgress = progress;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("progressToken", token);
        params.put("progress", progress);

        if (total != null)
            params.put("total", total);

        if (message != null)
            params.put("message", message);

        notify(new RpcNotification("notifications/progress", params));
    }

    /**
     * Emits a log notification only when it satisfies the request's advertised log threshold.
     *
     * @param level MCP logging level
     * @param data  JSON-compatible log payload
     */
    public void log(String level, Object data) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("level", level);
        params.put("data", data);
        RpcNotification n = new RpcNotification("notifications/message", params);

        try {
            protocol.requireRequestNotification(request, n);
        } catch (IllegalArgumentException ex) {
            return;
        }

        notify(n);
    }

    /**
     * Emits a non-terminal message if the request has not ended.
     *
     * @param message notification or stream message to emit
     */
    public synchronized void emit(RpcMessage message) {
        if (!ended.get())
            connection.send(this, message, false);
    }

    /**
     * Emits exactly one terminal response and completes the request lifecycle future.
     *
     * @param message terminal success or error response
     */
    public synchronized void finish(RpcMessage message) {
        if (!ended.compareAndSet(false, true))
            return;

        try {
            connection.send(this, message, true);
        } finally {
            done.complete(null);
        }
    }

    /**
     * Idempotently marks the request cancelled, interrupts its worker task, and completes its lifecycle future.
     */
    public void cancel() {
        // Never acquire an output/context lock when closing: another request may be writing.
        cancelled = true;
        ended.set(true);

        if (task != null)
            task.cancel(true);

        done.complete(null);
    }
}
