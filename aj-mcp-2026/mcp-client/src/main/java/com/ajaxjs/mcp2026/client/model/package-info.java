/**
 * Internal client-side state records for pending requests, HTTP exchanges, and subscriptions.
 *
 * <p>The records are deliberately small and are owned by {@code McpClient} or its transport;
 * they are public only to keep transport implementations decoupled from the client package's
 * implementation details.</p>
 */
package com.ajaxjs.mcp2026.client.model;
