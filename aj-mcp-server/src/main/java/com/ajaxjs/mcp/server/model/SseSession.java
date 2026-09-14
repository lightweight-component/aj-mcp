package com.ajaxjs.mcp.server.model;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents sse session.
 */
public final class SseSession {
    /**
     * Holds the writer value.
     */
    private final PrintWriter writer;

    /**
     * Holds the closed value.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new sse session.
     *
     * @param writer the writer value.
     */
    public SseSession(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Executes the send data operation.
     *
     * @param data the data value.
     */
    public void sendData(String data) {
        sendFrame("data: " + data + "\n\n");
    }

    /**
     * Executes the send frame operation.
     *
     * @param frame the frame value.
     */
    public void sendFrame(String frame) {
        synchronized (writer) {
            if (closed.get())
                throw new IllegalStateException("SSE session is closed");

            writer.write(frame);
            writer.flush();

            if (writer.checkError())
                throw new IllegalStateException("SSE connection write failed");
        }
    }

    /**
     * Executes the is closed operation.
     *
     * @return the result of the is closed operation.
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Executes the close operation.
     */
    public void close() {
        synchronized (writer) {
            if (closed.compareAndSet(false, true))
                writer.close();
        }
    }
}