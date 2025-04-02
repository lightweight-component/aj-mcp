package com.ajaxjs.mcp;

import java.util.concurrent.CompletableFuture;

public class McpUtils {

    // 模拟 failedFuture 的静态方法
    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable); // 标记为异常完成

        return future;
    }

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
