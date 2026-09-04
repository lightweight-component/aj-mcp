package com.ajaxjs.mcp.server.cancellation;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;

import java.util.concurrent.CountDownLatch;

/**
 * Represents cancellable tools.
 */
@McpService
public class CancellableTools {
    /**
     * Holds the entered value.
     */
    public static volatile CountDownLatch entered;
    /**
     * Holds the release value.
     */
    public static volatile CountDownLatch release;

    public static void reset(int callers) {
        entered = new CountDownLatch(callers);
        release = new CountDownLatch(1);
    }

    @Tool
    public String blocking() {
        entered.countDown();
        try {
            release.await();
            return "completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
    }

    @Tool
    public String interruptedAtEntry() {
        return Boolean.toString(Thread.currentThread().isInterrupted());
    }
}
