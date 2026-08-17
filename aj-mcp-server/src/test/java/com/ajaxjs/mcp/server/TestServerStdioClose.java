package com.ajaxjs.mcp.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class TestServerStdioClose {
    @Test
    @Timeout(10)
    void closeUnblocksInputAndStopsWorkerThread() throws Exception {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        CloseAwareBlockingInputStream input = new CloseAwareBlockingInputStream();
        System.setIn(input);
        System.setOut(new PrintStream(new ByteArrayOutputStream()));

        ServerStdio transport = null;
        Thread serverThread = null;
        try {
            McpServer server = new McpServer();
            transport = new ServerStdio(server);
            server.setTransport(transport);

            serverThread = new Thread(server::start, "server-start-test");
            serverThread.start();

            waitUntilRunning(transport);
            assertTrue(transport.getInputThread().isAlive());

            transport.close();
            serverThread.join(2_000);

            assertFalse(transport.getInputThread().isAlive());
            assertFalse(serverThread.isAlive());
            assertFalse(transport.getRunning().get());
            assertTrue(transport.getInputThread().isDaemon());
            assertDoesNotThrow(transport::close);
        } finally {
            if (transport != null)
                transport.close();
            System.setIn(originalIn);
            System.setOut(originalOut);

            if (serverThread != null && serverThread.isAlive())
                serverThread.interrupt();
        }
    }

    private static void waitUntilRunning(ServerStdio transport) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (!transport.getRunning().get() && System.currentTimeMillis() < deadline)
            Thread.sleep(10);

        assertTrue(transport.getRunning().get(), "STDIO server did not start in time");
    }

    private static class CloseAwareBlockingInputStream extends InputStream {
        private boolean closed;

        @Override
        public synchronized int read() throws IOException {
            while (!closed) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for input", e);
                }
            }

            return -1;
        }

        @Override
        public synchronized void close() {
            closed = true;
            notifyAll();
        }
    }
}
