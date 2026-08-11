package com.ajaxjs.mcp.server.cancellation;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;

import java.util.concurrent.CountDownLatch;

@McpService
public class CancellableTools {
    public static volatile CountDownLatch entered;
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
