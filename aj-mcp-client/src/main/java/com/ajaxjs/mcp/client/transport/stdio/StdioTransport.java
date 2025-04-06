package com.ajaxjs.mcp.client.transport.stdio;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.InitializationNotification;
import com.ajaxjs.mcp.client.protocol.InitializeRequest;
import com.ajaxjs.mcp.client.transport.McpOperationHandler;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Builder
@Slf4j
public class StdioTransport implements McpTransport {
    private final List<String> command;

    private final Map<String, String> environment;

    private Process process;

    private ProcessIOHandler processIOHandler;

    private boolean logEvents;

    private volatile McpOperationHandler messageHandler;

    @Override
    public void start(McpOperationHandler messageHandler) {
        this.messageHandler = messageHandler;
        log.info("Starting process: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        if (environment != null)
            processBuilder.environment().putAll(environment);

        try {
            process = processBuilder.start();
//            process.onExit().thenRun(() -> {
//                log.debug("Subprocess has exited with code: {}", process.exitValue());
//            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        processIOHandler = new ProcessIOHandler(process, messageHandler, logEvents);
        // FIXME: where should we obtain the thread?
        new Thread(processIOHandler).start();
        new Thread(new ProcessStderrHandler(process)).start();
    }

    @Override
    public CompletableFuture<JsonNode> initialize(InitializeRequest operation) {
        String requestString = JsonUtil.toJson(operation);
        String initializationNotification = JsonUtil.toJson(new InitializationNotification());

        return execute(requestString, operation.getId())
                .thenCompose(originalResponse -> execute(initializationNotification, null)
                        .thenCompose(nullNode -> CompletableFuture.completedFuture(originalResponse)));
    }

    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(ClientMessage operation) {
        return execute(JsonUtil.toJson(operation), operation.getId());
    }

    @Override
    public void executeOperationWithoutResponse(ClientMessage operation) {
        execute(JsonUtil.toJson(operation), null);
    }

    @Override
    public void close() throws IOException {
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
            messageHandler.startOperation(id, future);

        try {
            processIOHandler.submit(request);
            // For messages with null ID, we don't wait for a corresponding response
            if (id == null)
                future.complete(null);
        } catch (IOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}
