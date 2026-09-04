package com.ajaxjs.mcp.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Represents test initialize base.
 */
public abstract class TestInitializeBase {
    /**
     * Holds the mcp client value.
     */
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
