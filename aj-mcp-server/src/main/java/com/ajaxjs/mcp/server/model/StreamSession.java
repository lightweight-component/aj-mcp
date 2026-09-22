package com.ajaxjs.mcp.server.model;

import lombok.Getter;

import java.io.PrintWriter;

/**
 * Represents the optional GET event stream associated with a Streamable HTTP session.
 * <p>
 * POST requests may still be handled without this object; when present, it is used to deliver
 * asynchronous server messages as SSE frames on the long-lived GET response.
 */
public final class StreamSession {
    /**
     * Response writer for the long-lived SSE stream.
     */
    @Getter
    private final PrintWriter writer;

    /**
     * Creates a stream wrapper around an open HTTP response writer.
     *
     * @param writer the writer connected to the GET response body.
     */
    public StreamSession(PrintWriter writer) {
        if (writer == null)
            throw new IllegalArgumentException("writer is required");

        this.writer = writer;
    }

    /**
     * Sends a JSON-RPC message as a named SSE {@code message} event.
     *
     * @param json serialized JSON-RPC payload.
     */
    public void send(String json) {
        frame("event: message\ndata: " + json + "\n\n");
    }

    /**
     * Writes one complete SSE frame and detects suppressed writer errors.
     *
     * @param frame complete SSE frame including its trailing blank line.
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
     * Closes the underlying response writer. The surrounding HTTP session may continue to exist
     * and accept POST requests after the optional GET stream is closed.
     */
    public void close() {
        synchronized (writer) {
            writer.close();
        }
    }
}