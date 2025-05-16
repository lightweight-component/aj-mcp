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

@Builder
@Slf4j
public class StdioTransport extends McpTransport {
    private final List<String> command;

    private final Map<String, String> environment;

    private Process process;

    private PrintStream out;

    private boolean logEvents;

    @Override
    public void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
        setPendingRequests(pendingRequest);

        log.info("Starting process: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        if (environment != null)
            processBuilder.environment().putAll(environment);

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            log.warn("IOException when creating Process.", e);
            throw new UncheckedIOException(e);
        }

        // FIXME: where should we obtain the thread?
        new Thread(() -> {
            out = new PrintStream(process.getOutputStream(), true);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;

                while ((line = reader.readLine()) != null) {
//                    if (logEvents)
//                        log.info("Got result from Stream {}", line);

                    handle(JsonUtils.json2Node(line));
                }
            } catch (IOException e) {
                log.warn("IOException when creating Stdio.", e);
                throw new RuntimeException(e);
            }

            log.info("ProcessIOHandler has finished reading output from process"); // Why can't reach this line code
        }).start();

        new Thread(() -> { // Process for the Error output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;

                while ((line = reader.readLine()) != null)
                    log.warn("[ERROR] {}", line);
            } catch (IOException e) {
                log.warn("IOException when creating Error Stdio.", e);
                throw new UncheckedIOException(e);
            }
        }).start();
    }

    @Override
    public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
        String requestString = JsonUtils.toJson(request);
        String initializationNotification = JsonUtils.toJson(new InitializationNotification());

        return execute(requestString, request.getId())
                .thenCompose(originalResponse -> execute(initializationNotification, null)
                        .thenCompose(nullNode -> CompletableFuture.completedFuture(originalResponse)));
    }

    @Override
    public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
        return execute(JsonUtils.toJson(request), request.getId());
    }

    @Override
    public void sendRequestWithoutResponse(McpRequest request) {
        execute(JsonUtils.toJson(request), null);
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

        if (id != null)
//            messageHandler.startOperation(id, future);
            saveRequest(id, future);

        try {
            if (logEvents)
                log.debug("> {}", request);

            out.println(request); // 输入命令
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
        if (!process.isAlive())
            throw new IllegalStateException("Process is not alive");
    }

    @Override
    public void close() {
        out.close();
        process.destroy();
    }
}
