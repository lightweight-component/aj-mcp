package com.ajaxjs.mcp2026.server.model;

import com.ajaxjs.mcp2026.core.*;
import lombok.Getter;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Transport-scoped MCP connection that correlates requests, serializes outbound messages, and owns cancellation.
 * It has no protocol-session state: each request carries its own version and capabilities in metadata.
 */
@Getter
public class Connection implements AutoCloseable {
    /**
     * Serialized outbound message sink owned by the transport.
     */
    private final Sink sink;
    /**
     * Authenticated peer principal, or {@code null} for anonymous transports.
     */
    private final Principal principal;
    /**
     * Whether this connection follows HTTP stream-specific protocol constraints.
     */
    private final boolean http;
    /**
     * In-flight requests keyed by JSON-RPC ID for correlation and cancellation.
     */
    private final ConcurrentMap<RequestId, Context> active = new ConcurrentHashMap<>();
    /**
     * Idempotent connection-close state.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * Server callbacks implementing dispatch, scheduling, reporting, and lifecycle ownership.
     */
    private final ConnectionRuntime runtime;
    /**
     * Shared validator for inbound requests, notifications, and outbound result shapes.
     */
    private final ProtocolSchema protocol;

    /**
     * Creates one connection bound to a transport sink and server-owned runtime services.
     *
     * @param sink      serialized outbound transport sink
     * @param principal authenticated principal, or {@code null}
     * @param http      whether HTTP stream rules apply
     * @param runtime   server-owned dispatch and lifecycle facilities
     */
    public Connection(Sink sink, Principal principal, boolean http, ConnectionRuntime runtime) {
        this.sink = sink;
        this.principal = principal;
        this.http = http;
        this.runtime = runtime;
        this.protocol = runtime.getProtocol();
    }

    /**
     * Accepts one decoded transport message. Completion tracks request lifetime rather than dispatch only;
     * notifications always return without a JSON-RPC response.
     *
     * @param message decoded inbound message
     * @return lifecycle future for a request, or an already-completed future for a notification/ignored message
     */
    public CompletableFuture<Void> receive(RpcMessage message) {
        if (stopped.get()) throw new IllegalStateException("Connection closed");

        if (message instanceof RpcNotification) {
            try {
                protocol.requireClientNotification((RpcNotification) message);
                if (!http) cancel(RequestId.from(((RpcNotification) message).getParams().get("requestId")));
            } catch (RuntimeException ex) {
                runtime.report(ex);
            }

            return CompletableFuture.completedFuture(null);
        }

        if (!(message instanceof RpcRequest))
            return CompletableFuture.completedFuture(null);

        RpcRequest request = (RpcRequest) message;
        Context context = new Context(this, request, protocol);

        if (active.size() >= 128) {
            send(new RpcError(request.getId(), ErrorCodes.INTERNAL_ERROR, "Connection request limit exceeded"));
            return CompletableFuture.completedFuture(null);
        }

        if (active.putIfAbsent(request.getId(), context) != null) {
            // A second response with the same ID would corrupt the original caller's correlation.
            close();
            throw new IllegalArgumentException("Duplicate in-flight request ID");
        }

        context.getDone().whenComplete((v, e) -> {
            active.remove(request.getId(), context);
            runtime.subscriptionClosed(context);

            if (context.getAlarm() != null)
                context.getAlarm().cancel(false);
        });

        try {
            protocol.requireRequest(request);
            boolean listen = Subscriptions.LISTEN.equals(request.getMethod());
            Runnable expire = () -> {
                if (listen)
                    context.finish(Subscriptions.closed(request));
                else
                    context.finish(new RpcError(request.getId(), ErrorCodes.INTERNAL_ERROR, "Server execution deadline exceeded"));

                if (context.getTask() != null)
                    context.getTask().cancel(true);
            };

            if (listen) {
                synchronized (context) {
                    Map<String, Object> requested = Subscriptions.filter(request.getParams().get("notifications"));
                    Map<String, Object> accepted = new LinkedHashMap<>();

                    for (String key : Arrays.asList("toolsListChanged", "promptsListChanged", "resourcesListChanged", "resourceSubscriptions"))
                        if (requested.containsKey(key))
                            accepted.put(key, requested.get(key));

                    context.setAck(Subscriptions.acknowledge(request, accepted));
                    context.emit(context.getAck());
                    runtime.subscriptionOpened(context);

                    if (context.isCancelled())
                        runtime.subscriptionClosed(context);

                    context.setAlarm(runtime.schedule(expire, runtime.getSubscriptionLifetimeMs()));
                }
            } else {
                context.setAlarm(runtime.schedule(expire, runtime.getRequestTimeoutMs()));
                context.setTask(runtime.execute(() -> {
                    if (context.isCancelled())
                        return;

                    try {
                        McpResult result = runtime.dispatch(context);
                        RpcSuccess response = new RpcSuccess(request.getId(), result);
                        protocol.requireResult(request, response);
                        context.finish(response);
                    } catch (McpProtocolException ex) {
                        runtime.report(ex);
                        RpcError error = ex.getError();
                        context.finish(new RpcError(request.getId(), error.getCode(), error.getMessage(), error.getData()));
                    } catch (Throwable ex) {
                        if (ex instanceof InterruptedException)
                            Thread.currentThread().interrupt();

                        runtime.report(ex);

                        if (ex instanceof IllegalArgumentException && ex.getCause() instanceof McpProtocolException)
                            context.finish(((McpProtocolException) ex.getCause()).getError());
                        else
                            context.finish(new RpcError(request.getId(), ErrorCodes.INTERNAL_ERROR, "Handler failed"));
                    }
                }));

                if (context.isCancelled())
                    context.getTask().cancel(true);
            }

            if (context.getDone().isDone() && context.getAlarm() != null)
                context.getAlarm().cancel(false);
        } catch (McpProtocolException ex) {
            context.finish(ex.getError());
        } catch (RuntimeException ex) {
            runtime.report(ex);
            context.finish(new RpcError(request.getId(), ErrorCodes.INTERNAL_ERROR, "Dispatch failed"));
        }

        if (stopped.get())
            context.cancel();

        return context.getDone();
    }

    /**
     * Sends an unscoped response while serializing access to the transport sink.
     *
     * @param message response to send
     */
    private void send(RpcMessage message) {
        try {
            synchronized (sink) {
                if (!stopped.get()) sink.send(message);
            }
        } catch (IOException | RuntimeException ex) {
            close();
            runtime.report(ex);
        }
    }

    /**
     * Sends a context-bound stream or terminal message unless cancellation or closure owns the outcome.
     *
     * @param context  request context owning the message
     * @param message  message to send
     * @param terminal whether this is the final response
     */
    public void send(Context context, RpcMessage message, boolean terminal) {
        try {
            synchronized (sink) {
                if (!stopped.get() && !context.isExplicitlyCancelled() && (terminal || !context.getEnded().get()))
                    sink.send(message);
            }
        } catch (IOException | RuntimeException ex) {
            close();
            runtime.report(ex);
        }
    }

    /**
     * Cancels the active request with the supplied identifier if it still belongs to this connection.
     *
     * @param id request identifier
     */
    public void cancel(RequestId id) {
        Context context = active.get(id);

        if (context != null)
            context.cancel();
    }

    /**
     * Returns the number of currently active correlated requests.
     *
     * @return active request count
     */
    public int activeCount() {
        return active.size();
    }

    @Override
    public void close() {
        if (stopped.compareAndSet(false, true)) {
            for (Context context : active.values())
                context.cancel();

            runtime.connectionClosed(this);
        }
    }
}
