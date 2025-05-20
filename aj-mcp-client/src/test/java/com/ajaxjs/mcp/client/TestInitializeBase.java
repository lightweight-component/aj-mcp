package com.ajaxjs.mcp.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class TestInitializeBase {
    static IMcpClient mcpClient;

    @Test
    public void testInitialize() {
        assertNotNull(mcpClient);
    }

    @Test
    public void testInitializeCfg() {
        assertNotNull(mcpClient);
    }

    @Test
    public void testPing() {
        mcpClient.checkHealth();
    }
}
