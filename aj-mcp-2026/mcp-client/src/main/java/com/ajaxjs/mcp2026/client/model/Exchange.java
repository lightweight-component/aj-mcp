package com.ajaxjs.mcp2026.client.model;

import lombok.Getter;
import lombok.Setter;

import java.net.HttpURLConnection;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mutable ownership record for one HTTP request/response stream.
 * Cancellation may race connection setup, so the task and connection are published independently.
 */
@Getter
@Setter
public final class Exchange {
    /**
     * Cancellation marker checked before and during connection setup so a cancellation racing submission
     * does not continue an HTTP exchange.
     */
    final AtomicBoolean cancelled = new AtomicBoolean();

    /**
     * Connection published after AJ HTTP initialization; cancellation disconnects it asynchronously when present.
     */
    volatile HttpURLConnection connection;

    /**
     * I/O worker task published after submission; cancellation interrupts it when present.
     */
    volatile Future<?> task;
}
