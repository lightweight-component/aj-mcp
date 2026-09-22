package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * STDIO transport for embedding the server in command-line MCP integrations.
 * <p>
 * Each input line must contain one JSON-RPC message encoded as UTF-8. Protocol responses are
 * written only to stdout, while diagnostics use logging so they do not corrupt the MCP stream.
 */
@Data
@Slf4j
public class ServerStdio implements McpTransportSync {
    /**
     * Raw STDIO input stream supplied by the host process.
     */
    private final InputStream input = System.in;

    /**
     * UTF-8 line reader for newline-delimited JSON-RPC messages.
     */
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

    /**
     * UTF-8 stdout writer reserved exclusively for JSON-RPC protocol output.
     */
    private final PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);

    /**
     * Whether the input loop should keep accepting messages.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Guards against starting the same transport more than once.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Idempotent close marker shared by lifecycle and I/O paths.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Monitor protecting start/close transitions.
     */
    private final Object lifecycleLock = new Object();

    /**
     * Background thread that reads STDIO and submits work to the request executor.
     */
    private volatile Thread inputThread;

    /**
     * Bounded worker pool that prevents long-running tools from blocking cancellation input.
     */
    private final ExecutorService requestExecutor;

    /**
     * Server dispatcher that handles decoded JSON-RPC messages.
     */
    private McpServer server;

    /**
     * Creates a STDIO transport using concurrency limits from the server configuration.
     *
     * @param server the server dispatcher to invoke for each JSON-RPC message.
     */
    public ServerStdio(McpServer server) {
        this.server = server;
        ServerConfig config = server.getServerConfig() == null ? new ServerConfig() : server.getServerConfig();
        int workers = config.getStdioWorkers();
        int capacity = config.getStdioQueueCapacity();
        if (workers < 1 || capacity < 1)
            throw new IllegalArgumentException("STDIO worker count and queue capacity must be positive");
        requestExecutor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), runnable -> {
            Thread thread = new Thread(runnable, "aj-mcp-server-stdio-request");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void start() {
        synchronized (lifecycleLock) {
            if (closed.get())
                throw new IllegalStateException("STDIO server transport is closed");

            if (!started.compareAndSet(false, true))
                throw new IllegalStateException("STDIO server transport is already started");

            running.set(true);
            inputThread = new Thread(this::processInput, "aj-mcp-server-stdio-input");
            inputThread.setDaemon(true);
            inputThread.start();
        }

        try {
            while (running.get() && inputThread.isAlive())
                inputThread.join(250);
        } catch (InterruptedException e) {
            try {
                close();
            } catch (IOException closeError) {
                log.warn("Failed to close STDIO server transport after interruption", closeError);
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reads newline-delimited JSON-RPC messages until EOF, shutdown, or an I/O failure occurs.
     */
    private void processInput() {
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                final String message = line;
                // Dispatch requests concurrently so the input loop remains able to
                // receive notifications/cancelled while a tool is still running.
                dispatchLine(message);
            }
        } catch (IOException e) {
            if (!closed.get())
                log.warn("输入处理错误: {}", e.getMessage());
        } finally {
            requestExecutor.shutdown();
            try {
                // EOF means no cancellation can arrive anymore. Wait for all accepted
                // requests so start() does not return before their responses are flushed.
                if (!requestExecutor.awaitTermination(30, TimeUnit.SECONDS))
                    requestExecutor.shutdownNow();
            } catch (InterruptedException e) {
                requestExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            running.set(false);
        }
    }

    /**
     * Handles one raw JSON-RPC line and suppresses responses for valid notifications.
     *
     * @param line one complete JSON-RPC message read from stdin.
     */
    private void processLine(String line) {
        boolean expectsResponse = false;
        try {
            JsonNode envelope = JsonUtils.json2Node(line);
            // A structurally invalid JSON-RPC envelope still requires an error with
            // id=null; only a valid method without id is notification-shaped.
            expectsResponse = envelope.has("id") || !envelope.has("method")
                    || !"2.0".equals(envelope.path("jsonrpc").asText());
        } catch (RuntimeException ignored) {
            // Parse errors have no usable id but JSON-RPC still requires an error response.
            expectsResponse = true;
        }
        try {
            String response = handle(line);
            if (response != null)
                send("stdio", response);
        } catch (JsonRpcErrorException e) {
            if (expectsResponse)
                send("stdio", e.toJson());
        } catch (Exception e) {
            log.warn("Message processing error: {}", e.getMessage());
            if (expectsResponse)
                send("stdio", new JsonRpcErrorException(JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage()).toJson());
        }
    }

    /**
     * Keeps handshake, cancellation and reverse responses flowing even when all tool workers are busy.
     */
    private void dispatchLine(String message) {
        JsonNode envelope;
        try {
            envelope = JsonUtils.json2Node(message);
            String method = envelope.path("method").asText("");
            if (!envelope.has("id") || !envelope.has("method") || "initialize".equals(method)
                    || "ping".equals(method)) {
                processLine(message);
                return;
            }
        } catch (RuntimeException e) {
            processLine(message);
            return;
        }
        try {
            requestExecutor.execute(() -> processLine(message));
        } catch (RejectedExecutionException e) {
            Object id = JsonUtils.convertValue(envelope.get("id"), Object.class);
            send("stdio", new JsonRpcErrorException(id, JsonRpcErrorCode.INTERNAL_ERROR,
                    "MCP server is busy; retry later").toJson());
        }
    }

    @Override
    public String handle(String rawJson) {
        if (server.acceptClientResponse("stdio", rawJson))
            return null;
        McpRequestRawInfo request = McpServerInitialize.jsonRpcValidate(rawJson); // 解析输入消息
        server.bindSession("stdio");
        McpResponse mcpResponse;
        try {
            mcpResponse = server.processMessage(request);
        } finally {
            server.clearSession();
        }

        return mcpResponse == null ? null : JsonUtils.toJson(mcpResponse);  // 处理消息并生成响应
    }

    @Override
    public void send(String sessionId, String json) {
        synchronized (writer) {
            // MCP stdio accepts CRLF and LF; CRLF keeps output stable across JDKs
            // and preserves compatibility with the original transport behavior.
            writer.print(json);
            writer.print("\r\n");
            writer.flush();
            if (writer.checkError())
                throw new IllegalStateException("STDIO response writer failed");
        }
    }

    @Override
    public void broadcast(String json) {
        send("stdio", json);
    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true))
            return;

        running.set(false);
        server.removeSession("stdio");
        requestExecutor.shutdownNow();
        IOException closeFailure = null;

        try {
            // BufferedReader.readLine() holds the reader lock while blocking. Closing the
            // underlying stream directly is therefore required to unblock the input thread.
            input.close();
        } catch (IOException e) {
            closeFailure = e;
        }

        writer.flush();

        Thread thread;
        synchronized (lifecycleLock) {
            thread = inputThread;
        }

        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
            boolean interrupted = false;

            try {
                thread.join(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                interrupted = true;
            }

            if (thread.isAlive())
                log.warn("STDIO server input thread did not stop within 2 seconds");

            if (interrupted)
                Thread.currentThread().interrupt();
        }

        try {
            reader.close();
        } catch (IOException e) {
            if (closeFailure == null)
                closeFailure = e;
            else
                closeFailure.addSuppressed(e);
        }

        if (closeFailure != null)
            throw closeFailure;
    }
}
