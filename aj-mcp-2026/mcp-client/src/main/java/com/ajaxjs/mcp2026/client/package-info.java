/**
 * Stateless MCP client APIs and asynchronous transport adapters.
 *
 * <p>A client instance owns exactly one transport, request correlation state, deadlines,
 * subscription lifecycles, and optional multi-round-trip execution.  Implementations in this
 * package keep connection-specific state local and never perform an initialization handshake.</p>
 */
package com.ajaxjs.mcp2026.client;
