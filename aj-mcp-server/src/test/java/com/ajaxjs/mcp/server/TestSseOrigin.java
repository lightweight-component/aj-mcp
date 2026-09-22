package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.server.common.ServerConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSseOrigin {
    @Test
    void suppliedOriginsRequireExactExplicitPermission() {
        McpServer server = new McpServer();
        ServerConfig config = new ServerConfig();
        server.setServerConfig(config);
        ServerSse sse = new ServerSse(server);
        assertTrue(sse.isOriginAllowed(null));
        assertFalse(sse.isOriginAllowed("https://example.com"));
        config.setAllowedOrigins(Arrays.asList("https://example.com", "*", "null"));
        assertTrue(sse.isOriginAllowed("https://example.com"));
        assertFalse(sse.isOriginAllowed("https://example.com.evil.test"));
        assertFalse(sse.isOriginAllowed("http://example.com"));
        assertFalse(sse.isOriginAllowed("https://other.test"));
        assertFalse(sse.isOriginAllowed("null"));
        assertFalse(sse.isOriginAllowed("*"));
        assertFalse(sse.isOriginAllowed(""));
    }
}
