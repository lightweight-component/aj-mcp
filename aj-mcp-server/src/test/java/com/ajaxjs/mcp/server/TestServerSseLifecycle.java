package com.ajaxjs.mcp.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.*;

class TestServerSseLifecycle {
    @Test
    void closeStopsHeartbeatAndClosesConnections() throws Exception {
        ServerSse transport = new ServerSse(new McpServer());
        TrackingWriter output = new TrackingWriter(false);
        transport.addConnections("client", new PrintWriter(output));

        transport.heartbeat();
        assertTrue(transport.isHeartbeatRunning());
        transport.heartbeat();

        assertTrue(transport.isHeartbeatRunning());

        transport.close();

        assertTrue(output.closed);
        assertEquals(0, transport.getSessionCount());
        assertTrue(transport.isClosed());
        assertFalse(transport.isHeartbeatRunning());
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

        assertEquals(0, transport.getSessionCount());
        assertTrue(output.closed);
    }

    @Test
    void sessionOpeningAndResponsesAreTargeted() throws Exception {
        ServerSse transport = new ServerSse(new McpServer());
        StringWriter firstOutput = new StringWriter();
        StringWriter secondOutput = new StringWriter();

        transport.openSession("first", new PrintWriter(firstOutput), "message?uuid=first");
        transport.openSession("second", new PrintWriter(secondOutput), "message?uuid=second");
        transport.returnMessage("first", "{\"id\":1}");

        assertEquals("event: endpoint\ndata: message?uuid=first\n\n"
                + "event: message\ndata: {\"id\":1}\n\n", firstOutput.toString());
        assertEquals("event: endpoint\ndata: message?uuid=second\n\n", secondOutput.toString());
        assertTrue(transport.isSessionOpen("first"));

        transport.removeConnection("first");
        assertFalse(transport.isSessionOpen("first"));
        transport.close();
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
