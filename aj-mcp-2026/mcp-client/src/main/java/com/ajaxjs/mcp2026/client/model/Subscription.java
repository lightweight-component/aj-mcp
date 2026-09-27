package com.ajaxjs.mcp2026.client.model;

import com.ajaxjs.mcp2026.core.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Client-side lifecycle for a {@code subscriptions/listen} request.
 * Acknowledgement must arrive before matching stream notifications; closing this object cancels the listen request.
 */
@Getter
public final class Subscription implements AutoCloseable {
    /**
     * Long-lived {@code subscriptions/listen} request that owns the subscription identifier.
     */
    private final RpcRequest request;

    /**
     * Application callback invoked only after acknowledgement and protocol/filter validation.
     */
    private final Consumer<RpcNotification> updates;

    /**
     * Shared protocol validator supplied by the owning client.
     */
    private final ProtocolSchema protocol;

    /**
     * Future completed once with the server acknowledgement or exceptionally when listening ends first.
     */
    private final CompletableFuture<RpcNotification> acknowledged = new CompletableFuture<>();

    /**
     * Terminal future for the long-lived listen request, attached by the owning client after dispatch.
     */
    @Setter
    private volatile CompletableFuture<RpcSuccess> completion;

    /**
     * First accepted acknowledgement; its filter constrains all subsequent stream notifications.
     */
    private volatile RpcNotification ack;

    /**
     * Creates a subscription using an independent default protocol validator.
     *
     * @param request outbound {@code subscriptions/listen} request
     * @param updates consumer for validated post-acknowledgement notifications
     */
    public Subscription(RpcRequest request, Consumer<RpcNotification> updates) {
        this(request, updates, new ProtocolSchema());
    }

    /**
     * Constructs a subscription with the validator used for its stream.
     * The client passes its validator so all messages use one protocol policy.
     *
     * @param request  outbound {@code subscriptions/listen} request
     * @param updates  consumer for validated post-acknowledgement notifications
     * @param protocol validator shared with the owning client
     */
    public Subscription(RpcRequest request, Consumer<RpcNotification> updates, ProtocolSchema protocol) {
        this.request = request;
        this.updates = updates;
        this.protocol = protocol;
    }

    /**
     * Processes an acknowledgement or a notification associated with this subscription.
     * This method serializes acknowledgement state with delivery, rejects duplicates and rejects notifications
     * that precede acknowledgement or do not match the accepted filter.
     *
     * @param n received subscription notification
     */
    public synchronized void receive(RpcNotification n) {
        if (Subscriptions.ACKNOWLEDGED.equals(n.getMethod())) {
            if (ack != null)
                throw new IllegalArgumentException("Repeated acknowledgment");

            @SuppressWarnings("unchecked") Map<String, Object> filter = (Map<String, Object>) n.getParams().get("notifications");
            Subscriptions.acknowledge(request, filter);
            ack = n;
            acknowledged.complete(n);
        } else {
            if (ack == null)
                throw new IllegalArgumentException("Notification before acknowledgment");

            protocol.requireSubscriptionNotification(request, ack, n);
            updates.accept(n);
        }
    }

    /**
     * Returns the future completed by the server's acknowledgement notification.
     *
     * @return acknowledgement future, completed exceptionally if the listen request ends first
     */
    public CompletableFuture<RpcNotification> acknowledged() {
        return acknowledged;
    }

    /**
     * Returns the future for the terminal response of the long-lived listen request.
     *
     * @return completion future, or {@code null} until the owning client attaches it
     */
    public CompletableFuture<RpcSuccess> completion() {
        return completion;
    }

    @Override
    public void close() {
        if (completion != null)
            completion.cancel(true);
    }
}
