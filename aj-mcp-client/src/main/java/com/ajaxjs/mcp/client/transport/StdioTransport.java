package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializationNotification;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Builder
@Slf4j
public class StdioTransport extends McpTransport {
    private final List<String> command;

    private final Map<String, String> environment;

    private boolean logEvents;

    private Process process;

    private PrintStream out;

    private Thread stdoutReaderThread;

    private Thread stderrReaderThread;

    private volatile boolean closed;

    @Override
    public synchronized void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
        if (closed)
            throw new IllegalStateException("StdioTransport is closed");

        if (process != null)
            throw new IllegalStateException("StdioTransport is already started");

        setPendingRequests(pendingRequest);

        log.info("Starting process: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        if (environment != null)
            processBuilder.environment().putAll(environment);

        try {
            process = processBuilder.start();
            out = new PrintStream(process.getOutputStream(), true);
        } catch (IOException e) {
            log.warn("IOException when creating Process.", e);
            throw new UncheckedIOException(e);
        }

        Process startedProcess = process;
        stdoutReaderThread = new Thread(() -> {
            IOException channelFailure = null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(startedProcess.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
//                    if (logEvents)
//                        log.info("Got result from Stream {}", line);

                    handle(JsonUtils.json2Node(line));
                }
            } catch (IOException e) {
                if (!closed) {
                    channelFailure = e;
                    log.warn("IOException while reading MCP process output.", e);
                }
            } finally {
                if (!closed) {
                    if (channelFailure == null)
                        channelFailure = new IOException("MCP process stdout closed unexpectedly");
                    failPendingRequests(channelFailure);
                }
            }

            log.debug("MCP process stdout reader has stopped");
        }, "aj-mcp-stdio-stdout");
        stdoutReaderThread.setDaemon(true);
        stdoutReaderThread.start();

        stderrReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(startedProcess.getErrorStream()))) {
                String line;

                while ((line = reader.readLine()) != null)
                    log.warn("[ERROR] {}", line);
            } catch (IOException e) {
                if (!closed)
                    log.warn("IOException while reading MCP process error output.", e);
            }

            log.debug("MCP process stderr reader has stopped");
        }, "aj-mcp-stdio-stderr");
        stderrReaderThread.setDaemon(true);
        stderrReaderThread.start();
    }

    @Override
    public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
        String requestString = JsonUtils.toJson(request);
        String initializationNotification = JsonUtils.toJson(new InitializationNotification());

        return execute(requestString, numericId(request.getId()))
                .thenCompose(originalResponse -> execute(initializationNotification, null)
                        .thenCompose(nullNode -> CompletableFuture.completedFuture(originalResponse)));
    }

    @Override
    public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
        requireInitialized();

        return execute(JsonUtils.toJson(request), numericId(request.getId()));
    }

    @Override
    public void sendRequestWithoutResponse(McpRequest request) {
        execute(JsonUtils.toJson(request), null);
    }

    @Override
    protected void sendJson(JsonNode message) {
        execute(JsonUtils.toJson(message), null);
    }

    /**
     * 执行一个请求，异步返回响应（这里不使用发送请求了，因为是 stdio，使用执行表述更精确）
     * Executes a given request and returns the response asynchronously.
     * 异步方式采用 CompletableFuture.
     * This method uses CompletableFuture to handle asynchronous operations and process responses.
     *
     * @param request 要执行的请求 The request string to execute.
     * @param id      请求的 id。若为 null 则表示不需要处理响应。 The ID of the request. If null, it indicates that no response is expected for this request.
     * @return CompletableFuture<JsonNode> representing the asynchronous operation.
     * If id is null, the future completes immediately with a null value.
     * If an IOException occurs, the future completes exceptionally.
     */
    private CompletableFuture<JsonNode> execute(String request, Long id) {
        log.info("JSON RPC {}", request);
        CompletableFuture<JsonNode> future = new CompletableFuture<>();

        Process currentProcess = process;
        if (closed || out == null || currentProcess == null || !currentProcess.isAlive()) {
            future.completeExceptionally(new IllegalStateException("StdioTransport is not running"));
            return future;
        }

        if (id != null)
//            messageHandler.startOperation(id, future);
            saveRequest(id, future);

        try {
            if (logEvents)
                log.debug("> {}", request);

            out.println(request); // 输入命令
            if (out.checkError() || !currentProcess.isAlive()) {
                IOException failure = new IOException("Failed to write request because the MCP process has stopped");
                failPendingRequests(failure);
                future.completeExceptionally(failure);
                return future;
            }

            // 如果没有 id 的消息，那么表示不用等待响应 For messages with null ID, we don't wait for a corresponding response
            if (id == null)
                future.complete(null);
        } catch (Exception e) {
            log.warn("Exception when executing StdioTransport.", e);
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public void checkHealth() {
        if (closed || process == null || !process.isAlive())
            throw new IllegalStateException("Process is not alive");
    }

    @Override
    public synchronized void close() {
        if (closed)
            return;

        closed = true;
        failPendingRequests(new IOException("STDIO MCP transport is closed"));
        boolean interrupted = false;

        if (out != null)
            out.close();

        if (process != null) {
            process.destroy();

            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    if (!process.waitFor(2, TimeUnit.SECONDS))
                        log.warn("MCP process did not terminate after being forcibly destroyed");
                }
            } catch (InterruptedException e) {
                interrupted = true;
                process.destroyForcibly();
            }

            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            closeQuietly(process.getOutputStream());
        }

        if (stdoutReaderThread != null)
            stdoutReaderThread.interrupt();
        if (stderrReaderThread != null)
            stderrReaderThread.interrupt();

        interrupted |= joinQuietly(stdoutReaderThread);
        interrupted |= joinQuietly(stderrReaderThread);

        if (interrupted)
            Thread.currentThread().interrupt();
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Best-effort cleanup during shutdown.
        }
    }

    private static boolean joinQuietly(Thread thread) {
        if (thread == null || thread == Thread.currentThread())
            return false;

        try {
            thread.join(1_000);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }
}
