package com.ajaxjs.mcp.server.model;

import java.io.PrintWriter;

/**
 * Represents stream session.
 */
public final class StreamSession {
    /**
     * Holds the writer value.
     */
    private final PrintWriter writer;

    /**
     * Creates a new stream session.
     *
     * @param writer the writer value.
     */
    public StreamSession(PrintWriter writer) {
        if (writer == null)
            throw new IllegalArgumentException("writer is required");

        this.writer = writer;
    }

    /**
     * Executes the send operation.
     *
     * @param json the json value.
     */
    public void send(String json) {
        synchronized (writer) {
            writer.write("event: message\ndata: " + json + "\n\n");
            writer.flush();

            if (writer.checkError())
                throw new IllegalStateException("Streamable HTTP SSE write failed");
        }
    }

    /**
     * Executes the close operation.
     */
    public void close() {
        synchronized (writer) {
            writer.close();
        }
    }
}