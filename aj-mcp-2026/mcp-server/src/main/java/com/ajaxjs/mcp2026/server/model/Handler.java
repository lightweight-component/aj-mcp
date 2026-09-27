package com.ajaxjs.mcp2026.server.model;

import com.ajaxjs.mcp2026.core.McpResult;

/**
 * Application callback that handles one validated MCP request within its isolated context.
 */
@FunctionalInterface
public interface Handler {
    /**
     * Produces the result for one request.
     *
     * @param context connection-local execution context, including principal and cancellation state
     * @return protocol-ready result to place in the correlated success response
     * @throws Exception if application processing cannot complete normally
     */
    McpResult handle(Context context) throws Exception;
}
