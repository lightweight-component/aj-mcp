package com.ajaxjs.mcp.server.model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks a request that is currently executing on a worker thread.
 * <p>
 * Cancellation is cooperative: the first cancel call records the state and interrupts the
 * worker thread so blocking operations can react according to normal Java interruption rules.
 */
public final class RunningRequest {
    /**
     * Worker thread currently processing the JSON-RPC request.
     */
    private final Thread thread;

    /**
     * Ensures cancellation and interruption are performed only once.
     */
    private final AtomicBoolean cancelled = new AtomicBoolean();

    /**
     * Creates a request tracker for the supplied worker thread.
     *
     * @param thread the worker thread to interrupt on cancellation.
     */
    public RunningRequest(Thread thread) {
        this.thread = thread;
    }

    /**
     * Marks the request as cancelled and interrupts its worker thread on the first call.
     */
    public void cancel() {
        if (cancelled.compareAndSet(false, true))
            thread.interrupt();
    }
}