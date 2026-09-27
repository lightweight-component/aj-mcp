package com.ajaxjs.mcp2026.server.model;

import com.ajaxjs.mcp2026.core.McpResult;
import com.ajaxjs.mcp2026.core.ProtocolSchema;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * Server-owned facilities required by one connection.
 * Connection state remains isolated; executors, subscriptions and reporting
 * remain owned by the enclosing server instance.
 */
public interface ConnectionRuntime {
    /**
     * Returns the shared protocol validator for this server instance.
     *
     * @return protocol validator
     */
    ProtocolSchema getProtocol();

    /**
     * Returns the maximum ordinary request lifetime.
     *
     * @return timeout in milliseconds
     */
    long getRequestTimeoutMs();

    /**
     * Returns the maximum subscription lifetime.
     *
     * @return timeout in milliseconds
     */
    long getSubscriptionLifetimeMs();

    /**
     * Schedules a lifecycle task owned by the server.
     *
     * @param task    task to execute
     * @param delayMs positive delay in milliseconds
     * @return cancellation handle
     */
    ScheduledFuture<?> schedule(Runnable task, long delayMs);

    /**
     * Submits an ordinary request handler to the bounded worker pool.
     *
     * @param task handler task
     * @return cancellation handle
     */
    Future<?> execute(Runnable task);

    /**
     * Dispatches one validated request to a registered or built-in MCP handler.
     *
     * @param context request execution context
     * @return handler result
     * @throws Exception when the handler cannot complete normally
     */
    McpResult dispatch(Context context) throws Exception;

    /**
     * Reports a local failure without allowing reporting itself to alter protocol processing.
     *
     * @param error local failure
     */
    void report(Throwable error);

    /**
     * Registers a successfully acknowledged subscription context.
     *
     * @param context active subscription context
     */
    void subscriptionOpened(Context context);

    /**
     * Removes a subscription context when it completes or is cancelled.
     *
     * @param context completed subscription context
     */
    void subscriptionClosed(Context context);

    /**
     * Removes a closed connection from server-owned lifecycle tracking.
     *
     * @param connection closed connection
     */
    void connectionClosed(Connection connection);
}
