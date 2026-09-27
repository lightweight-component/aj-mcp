/**
 * Stateless MCP server dispatcher and its STDIO and HTTP transport adapters.
 *
 * <p>{@code McpServer} owns registered capabilities and shared execution resources, while each
 * transport creates isolated {@code Connection} state for an authenticated peer and request stream.</p>
 */
package com.ajaxjs.mcp2026.server;
