package com.ajaxjs.mcp.server.model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents running request.
 */
public final class RunningRequest {
    /**
     * Holds the thread value.
     */
    private final Thread thread;

    /**
     * Holds the canceled value.
     */
    private final AtomicBoolean cancelled = new AtomicBoolean();

    /**
     * Creates a new running request.
     *
     * @param thread the thread value.
     */
    public RunningRequest(Thread thread) {
        this.thread = thread;
    }

    /**
     * Executes the cancel operation.
     */
    public void cancel() {
        if (cancelled.compareAndSet(false, true))
            thread.interrupt();
    }
}