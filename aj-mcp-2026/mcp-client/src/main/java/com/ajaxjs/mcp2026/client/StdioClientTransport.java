package com.ajaxjs.mcp2026.client;

import com.ajaxjs.mcp2026.core.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns supplied streams, or a launched subprocess. stderr is inherited, never merged into stdout.
 */
public final class StdioClientTransport implements ClientTransport {
    /**
     * Owned peer stdout stream carrying newline-delimited MCP messages.
     */
    private final InputStream input;
    /**
     * Owned peer stdin stream reserved exclusively for newline-delimited MCP messages.
     */
    private final OutputStream output;
    /**
     * Launched a peer process, or {@code null} when callers supplied the streams.
     */
    private final Process process;
    /**
     * Strict codec used at the STDIO protocol boundary.
     */
    private final McpCodec codec = new McpCodec();
    /**
     * Idempotent shutdown state shared by reader and writer tasks.
     */
    private final AtomicBoolean closed = new AtomicBoolean();
    /**
     * Requests awaiting the single writer hand-off, enabling pre-write cancellation.
     */
    private final ConcurrentMap<RequestId, AtomicBoolean> queued = new ConcurrentHashMap<>();
    /**
     * Single-threaded bounded writer preventing JSON line interleaving on stdout.
     */
    private final ExecutorService writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(256), McpClient.daemon("mcp-stdio-write"), new ThreadPoolExecutor.AbortPolicy());
    /**
     * Listener installed once before either direction processes protocol messages.
     */
    private Listener listener;
    /**
     * Daemon reader thread, interrupted and released when the transport closes.
     */
    private Thread reader;

    /**
     * Creates a transport over caller-supplied MCP stdin/stdout streams.
     * The transport closes both streams when closed and serializes writes so JSON lines cannot interleave.
     *
     * @param input  peer output carrying newline-delimited MCP messages
     * @param output peer input receiving newline-delimited MCP messages
     */
    public StdioClientTransport(InputStream input, OutputStream output) {
        this(input, output, null);
    }

    /**
     * Creates a transport and optionally records ownership of the launched peer process.
     *
     * @param input   peer stdout carrying protocol lines
     * @param output  peer stdin receiving protocol lines
     * @param process launched process to terminate on close, or {@code null} for caller-owned streams
     */
    private StdioClientTransport(InputStream input, OutputStream output, Process process) {
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
        this.process = process;
    }

    /**
     * Launches a peer process and connects its stdout/stdin to this transport.
     * Process stderr is inherited by the caller rather than mixed into the protocol stream.
     *
     * @param command executable and arguments for the MCP peer
     * @return transport owning the launched process and its protocol streams
     * @throws IOException if the process cannot be started
     */
    public static StdioClientTransport launch(List<String> command) throws IOException {
        Process process = new ProcessBuilder(new ArrayList<>(command)).redirectError(ProcessBuilder.Redirect.INHERIT).start();

        return new StdioClientTransport(process.getInputStream(), process.getOutputStream(), process);
    }

    /**
     * Starts the one reader thread and installs the sole listener for decoded peer messages and terminal failures.
     * Empty input lines are invalid because each STDIO line must contain exactly one MCP JSON-RPC message.
     *
     * @param listener recipient for decoded messages and transport-wide failures
     * @throws IllegalStateException if the transport was already started or closed
     */
    public synchronized void start(Listener listener) {
        if (this.listener != null || closed.get())
            throw new IllegalStateException("Transport already started/closed");

        this.listener = Objects.requireNonNull(listener);

        reader = McpClient.daemon("mcp-stdio-read").newThread(() -> {
            try {
                String line;

                while (!closed.get() && (line = WireIO.line(input)) != null) {
                    if (line.isEmpty()) throw new IOException("Empty STDIO message");
                    listener.message(null, codec.decode(line));
                }

                if (!closed.get())
                    listener.failed(null, new EOFException("STDIO peer exited"));
            } catch (Throwable ex) {
                if (!closed.get())
                    listener.failed(null, ex);
            }
        });

        reader.start();
    }

    /**
     * Queues a request for serialized newline-delimited output without blocking the caller on pipe I/O.
     * A request canceled before the writer hand-off is suppressed rather than written.
     *
     * @param request validated request to transmit
     * @throws IllegalStateException if the listener is not installed or the transport is closed
     */
    public void send(RpcRequest request) {
        if (closed.get() || listener == null)
            throw new IllegalStateException("Transport not open");

        AtomicBoolean cancelled = new AtomicBoolean();
        queued.put(request.getId(), cancelled);

        try {
            writer.execute(() -> {
                // Removal is the hand-off to the ordered writer: later cancellation follows this line.
                queued.remove(request.getId(), cancelled);

                if (cancelled.get() || closed.get())
                    return;

                try {
                    WireIO.writeLine(output, request, codec);
                } catch (Throwable ex) {
                    if (!closed.get())
                        listener.failed(null, ex);
                }
            });
        } catch (RuntimeException ex) {
            queued.remove(request.getId(), cancelled);

            throw ex;
        }
    }

    /**
     * Queues a non-request protocol message behind already queued STDIO writes.
     *
     * @param message notification or another message to serialize as one protocol line
     * @throws IllegalStateException if the transport is not open
     */
    private void enqueue(RpcMessage message) {
        if (closed.get() || listener == null)
            throw new IllegalStateException("Transport not open");

        writer.execute(() -> {
            try {
                if (!closed.get())
                    WireIO.writeLine(output, message, codec);
            } catch (Throwable ex) {
                if (!closed.get())
                    listener.failed(null, ex);
            }
        });
    }

    /**
     * Cancels a queued request locally, or sends {@code notifications/cancelled} once its write hand-off occurred.
     * STDIO has no request-scoped HTTP stream to disconnect, so peer cancellation is represented by the notification.
     *
     * @param id request identifier to cancel
     */
    public void cancel(RequestId id) {
        AtomicBoolean queuedRequest = queued.remove(id);

        if (queuedRequest != null) {
            queuedRequest.set(true);

            return;
        }

        if (!closed.get())
            try {
                enqueue(new RpcNotification("notifications/cancelled", Collections.singletonMap("requestId", id.getValue())));
            } catch (RuntimeException ignored) {
            }
    }

    /**
     * Idempotent stops the writer, clears queued requests, closes owned streams, and terminates a launched peer.
     * The reader is interrupted only after stream closure, so blocked reads are released without protocol output races.
     */
    public void close() {
        if (!closed.compareAndSet(false, true))
            return;

        writer.shutdownNow();
        queued.clear();
        // Close stdin first when the writer is idle; otherwise kill the child to release blocked pipe I/O.

        if (process != null) {
            try {
                if (writer.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    try {
                        output.close();
                    } catch (IOException ignored) {
                    }
                }
                if (!process.waitFor(500, TimeUnit.MILLISECONDS))
                    process.destroy();

                if (!process.waitFor(500, TimeUnit.MILLISECONDS))
                    process.destroyForcibly();

                process.waitFor(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        try {
            output.close();
        } catch (IOException ignored) {
        }

        try {
            input.close();
        } catch (IOException ignored) {
        }

        if (reader != null)
            reader.interrupt();
    }
}
