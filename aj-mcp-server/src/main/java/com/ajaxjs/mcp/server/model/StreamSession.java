package com.ajaxjs.mcp.server.model;

import lombok.Getter;

import java.io.PrintWriter;

/**
 * Represents stream session.
 */
public final class StreamSession {
    /**
     * Holds the writer value.
     */
    @Getter
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
        frame("event: message\ndata: " + json + "\n\n");
    }

    /**
     * Writes one complete SSE frame and detects suppressed writer errors.
     *
     * @param frame complete SSE frame including its trailing blank line
     */
    public void frame(String frame) {
        synchronized (writer) {
            writer.write(frame);
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