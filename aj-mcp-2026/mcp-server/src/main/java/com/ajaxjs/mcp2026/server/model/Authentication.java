package com.ajaxjs.mcp2026.server.model;

import com.sun.net.httpserver.HttpExchange;

import java.security.Principal;

/**
 * Authenticates one HTTP exchange before it is associated with an MCP connection.
 */
@FunctionalInterface
public interface Authentication {
    /**
     * Authenticates the request without relying on unvalidated protocol payload fields.
     *
     * @param exchange incoming HTTP exchange
     * @return authenticated principal, or {@code null} for an anonymous deployment
     * @throws SecurityException when authentication fails
     */
    Principal authenticate(HttpExchange exchange) throws SecurityException;
}