/**
 * Server-side connection, request-context, registration, and output abstractions.
 *
 * <p>These types separate per-connection lifecycle state from server-owned executors and
 * registries, preserving isolation between concurrent peers.</p>
 */
package com.ajaxjs.mcp2026.server.model;
