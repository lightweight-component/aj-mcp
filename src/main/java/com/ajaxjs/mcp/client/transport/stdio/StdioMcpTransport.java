package com.ajaxjs.mcp.client.transport.stdio;

import com.ajaxjs.mcp.McpUtils;
import com.ajaxjs.mcp.client.protocol.InitializationNotification;
import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.InitializeRequest;
import com.ajaxjs.mcp.client.transport.McpOperationHandler;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StdioMcpTransport implements McpTransport {
    private final List<String> command;
    private final Map<String, String> environment;
    private Process process;
    private ProcessIOHandler processIOHandler;
    private final boolean logEvents;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(StdioMcpTransport.class);
    private volatile McpOperationHandler messageHandler;

    public StdioMcpTransport(Builder builder) {
        this.command = builder.command;
        this.environment = builder.environment;
        this.logEvents = builder.logEvents;
    }

    @Override
    public void start(McpOperationHandler messageHandler) {
        this.messageHandler = messageHandler;
        log.debug("Starting process: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(environment);

        try {
            process = processBuilder.start();
            log.debug("PID of the started process: {}", ProcessStderrHandler.getPid(process));
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
        try {
            String requestString = OBJECT_MAPPER.writeValueAsString(operation);
            String initializationNotification = OBJECT_MAPPER.writeValueAsString(new InitializationNotification());
            return execute(requestString, operation.getId())
                    .thenCompose(originalResponse -> execute(initializationNotification, null)
                            .thenCompose(nullNode -> CompletableFuture.completedFuture(originalResponse)));
        } catch (JsonProcessingException e) {
            return McpUtils.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(ClientMessage operation) {
        try {
            String requestString = OBJECT_MAPPER.writeValueAsString(operation);

            return execute(requestString, operation.getId());
        } catch (JsonProcessingException e) {
            return McpUtils.failedFuture(e);
        }
    }

    @Override
    public void executeOperationWithoutResponse(ClientMessage operation) {
        try {
            String requestString = OBJECT_MAPPER.writeValueAsString(operation);
            execute(requestString, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        process.destroy();
    }

    private CompletableFuture<JsonNode> execute(String request, Long id) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        if (id != null)
            messageHandler.startOperation(id, future);

        try {
            processIOHandler.submit(request);
            // For messages with null ID, we don't wait for a corresponding response
            if (id == null) {
                future.complete(null);
            }
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public static class Builder {

        private List<String> command;
        private Map<String, String> environment;
        private boolean logEvents;

        public Builder command(List<String> command) {
            this.command = command;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public Builder logEvents(boolean logEvents) {
            this.logEvents = logEvents;
            return this;
        }

        public StdioMcpTransport build() {
            if (command == null || command.isEmpty())
                throw new IllegalArgumentException("Missing command");

            if (environment == null)
                environment = new HashMap<>();

            return new StdioMcpTransport(this);
        }
    }
}
