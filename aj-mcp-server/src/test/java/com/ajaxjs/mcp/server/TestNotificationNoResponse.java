package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.server.common.ServerConfig;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestNotificationNoResponse extends TestStdioServerBase {
    private static final String INITIALIZED_NOTIFICATION =
            "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n";

    @Test
    void stdioDoesNotWriteResponseForNotification() {
        setIn(INITIALIZED_NOTIFICATION);

        assertEquals("", testOut.toString());
    }

    @Test
    void sseDoesNotSerializeOrWriteResponseForNotification() {
        McpServer server = new McpServer();
        ServerConfig config = new ServerConfig();
        config.setStrictLifecycle(false);
        server.setServerConfig(config);
        ServerSse transport = new ServerSse(server);
        StringWriter output = new StringWriter();
        transport.addConnections("client", new PrintWriter(output));

        String response = transport.handle(INITIALIZED_NOTIFICATION);
        assertNull(response);
        transport.returnMessage("client", response);
        assertEquals("", output.toString());
    }
}
