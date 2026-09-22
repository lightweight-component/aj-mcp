package com.ajaxjs.mcp.client.transport;

import java.io.IOException;

/**
 * Structured HTTP failure; only an initial POST rejection may trigger transport discovery.
 */
final class HttpStatusException extends IOException {
    final int status;
    final boolean initialization;

    HttpStatusException(int status, boolean initialization, String body) {
        super("MCP HTTP " + status + ": " + body);
        this.status = status;
        this.initialization = initialization;
    }
}
