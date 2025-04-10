package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.initialize.InitializationNotification;
import com.ajaxjs.mcp.client.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Builder
@Slf4j
public class StdioTransport extends BaseTransport {
    private final List<String> command;

    private final Map<String, String> environment;

    private Process process;

    private PrintStream out;

    private boolean logEvents;

    @Override
    public void start(Map<Long, CompletableFuture<JsonNode>> pendingOperations) {
        setPendingOperations(pendingOperations);

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
    public CompletableFuture<JsonNode> initialize(InitializeRequest operation) {
        String requestString = JsonUtils.toJson(operation);
        String initializationNotification = JsonUtils.toJson(new InitializationNotification());

        return execute(requestString, operation.getId())
                .thenCompose(originalResponse -> execute(initializationNotification, null)
                        .thenCompose(nullNode -> CompletableFuture.completedFuture(originalResponse)));
    }

    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(ClientMessage operation) {
        return execute(JsonUtils.toJson(operation), operation.getId());
    }

    @Override
    public void executeOperationWithoutResponse(ClientMessage operation) {
        execute(JsonUtils.toJson(operation), null);
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

    /**
     * Executes a given request and returns the response asynchronously.
     * This method uses CompletableFuture to handle asynchronous operations and process responses.
     *
     * @param request The request string to execute.
     * @param id      The ID of the request. If null, it indicates that no response is expected for this request.
     * @return CompletableFuture<JsonNode> representing the asynchronous operation.
     * If id is null, the future completes immediately with a null value.
     * If an IOException occurs, the future completes exceptionally.
     */
    private CompletableFuture<JsonNode> execute(String request, Long id) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        log.info("JSON RPC {}", request);

        if (id != null)
//            messageHandler.startOperation(id, future);
            startOperation(id, future);

        try {
            if (logEvents)
                log.debug("> {}", request);

            out.println(request); // 输入命令
            // For messages with null ID, we don't wait for a corresponding response
            if (id == null)
                future.complete(null);
        } catch (Exception e) {
            log.warn("Exception when executing StdioTransport.", e);
            future.completeExceptionally(e);
        }

        return future;
    }
}
