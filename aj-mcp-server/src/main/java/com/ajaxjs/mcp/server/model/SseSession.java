package com.ajaxjs.mcp.server.model;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents one legacy HTTP/SSE client connection.
 * <p>
 * The session serializes all writes through the underlying {@link PrintWriter} so concurrent
 * notifications cannot interleave, and it checks {@link PrintWriter#checkError()} because
 * {@code PrintWriter} suppresses I/O exceptions.
 */
public final class SseSession {
    /**
     * HTTP response writer used for SSE frames.
     */
    private final PrintWriter writer;

    /**
     * Idempotent close flag shared by send and close paths.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a session around an already-open SSE response writer.
     *
     * @param writer the writer connected to the HTTP response body.
     */
    public SseSession(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Sends one default {@code data:} SSE event containing a serialized JSON-RPC payload.
     *
     * @param data serialized payload to write as the event data.
     */
    public void sendData(String data) {
        sendFrame("data: " + data + "\n\n");
    }

    /**
     * Writes one complete SSE frame and fails fast if the client connection is closed.
     *
     * @param frame complete SSE frame including the required trailing blank line.
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
     * Reports whether this SSE response has already been closed.
     *
     * @return {@code true} after the first successful close.
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes the SSE writer once. Further send attempts fail with an {@link IllegalStateException}.
     */
    public void close() {
        synchronized (writer) {
            if (closed.compareAndSet(false, true))
                writer.close();
        }
    }
}