package com.ajaxjs.mcp.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestServerSseLifecycle {
    @Test
    void closeStopsHeartbeatAndClosesConnections() throws Exception {
        ServerSse transport = new ServerSse(new McpServer());
        TrackingWriter output = new TrackingWriter(false);
        transport.addConnections("client", new PrintWriter(output));

        transport.heartbeat();
        Object executor = transport.getHeartbeatExecutor();
        transport.heartbeat();

        assertNotNull(executor);
        assertEquals(executor, transport.getHeartbeatExecutor());

        transport.close();

        assertTrue(output.closed);
        assertTrue(transport.getConnections().isEmpty());
        assertTrue(transport.getClosed().get());
        assertThrows(IllegalStateException.class,
                () -> transport.addConnections("late", new PrintWriter(new TrackingWriter(false))));
        transport.close();
    }

    @Test
    void failedPrintWriterIsRemovedAfterBroadcast() {
        ServerSse transport = new ServerSse(new McpServer());
        TrackingWriter output = new TrackingWriter(true);
        transport.addConnections("broken", new PrintWriter(output));

        transport.broadcast("heartbeat");

        assertFalse(transport.getConnections().containsKey("broken"));
        assertTrue(output.closed);
    }

    private static class TrackingWriter extends Writer {
        private final boolean failWrites;
        private boolean closed;

        private TrackingWriter(boolean failWrites) {
            this.failWrites = failWrites;
        }

        @Override
        public void write(char[] chars, int offset, int length) throws IOException {
            if (failWrites)
                throw new IOException("connection lost");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
