package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.core.McpCodec;
import com.ajaxjs.mcp2026.core.McpProtocolException;
import com.ajaxjs.mcp2026.core.RpcError;
import com.ajaxjs.mcp2026.core.WireIO;
import com.ajaxjs.mcp2026.server.model.Connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Blocking input loop; business handlers execute on the server's bounded worker pool. Owns both streams.
 */
public final class StdioServerTransport implements AutoCloseable {
    /**
     * Instance-scoped dispatcher that performs protocol validation and handler execution.
     */
    private final McpServer server;

    /**
     * Owned peer input containing newline-delimited JSON-RPC messages.
     */
    private final InputStream input;

    /**
     * Owned peer output reserved exclusively for newline-delimited JSON-RPC messages.
     */
    private final OutputStream output;

    /**
     * Strict codec used at the STDIO wire boundary.
     */
    private final McpCodec codec = new McpCodec();

    /**
     * Idempotent transport shutdown state.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * One-shot guard for the blocking input loop.
     */
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * Connection created for this stream and closed with the transport.
     */
    private volatile Connection connection;

    /**
     * Creates a blocking STDIO transport. The transport owns both streams and reserves stdout exclusively for MCP JSON.
     *
     * @param server server dispatcher
     * @param input  peer input carrying newline-delimited JSON-RPC messages
     * @param output peer output receiving newline-delimited JSON-RPC messages
     */
    public StdioServerTransport(McpServer server, InputStream input, OutputStream output) {
        this.server = Objects.requireNonNull(server);
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
    }

    /**
     * Runs the input loop until EOF, close, or an I/O failure; individual malformed notifications remain response-free.
     *
     * @throws IOException if a protocol line cannot be read or a permitted response cannot be written
     */
    public void run() throws IOException {
        if (!started.compareAndSet(false, true) || closed.get())
            throw new IllegalStateException("Transport already started/closed");

        connection = server.connect(message -> WireIO.writeLine(output, message, codec), null, false);

        try {
            String line;

            while (!closed.get() && (line = WireIO.line(input)) != null) {
                try {
                    connection.receive(codec.decode(line));
                } catch (McpProtocolException ex) {
                    RpcError error = ex.toErrorResponse();

                    if (error != null)
                        WireIO.writeLine(output, error, codec);
                }
            }
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (connection != null)
                connection.close();

            try {
                input.close();
            } catch (IOException ignored) {
            }

            try {
                output.close();
            } catch (IOException ignored) {
            }
        }
    }
}
