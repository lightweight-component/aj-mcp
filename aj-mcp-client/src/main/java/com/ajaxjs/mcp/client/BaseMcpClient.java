package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base Class for MCP Client, mainly doing the initialize job.
 */
@Slf4j
public abstract class BaseMcpClient implements IMcpClient, McpConstant {
    final McpTransport transport;

    /**
     * Sets the name that the client will use to identify itself to the MCP server in the initialization message.
     */
    final String clientName;

    /**
     * Sets the version string that the client will use to identify itself to the MCP server in the initialization message. The default value is "1.0".
     */
    final String clientVersion;

    /**
     * Sets the protocol version that the client will advertise in the
     * initialization message. The default value right now is "2024-11-05", but will change over time in later versions.
     */
    final String protocolVersion;

    /**
     * Sets the timeout for tool execution.
     * This value applies to each tool execution individually.
     * The default value is 60 seconds.
     * A value of zero means no timeout.
     */
    final Duration requestTimeout;

    final Map<Long, CompletableFuture<JsonNode>> pendingOperations = new ConcurrentHashMap<>();

    final AtomicLong idGenerator = new AtomicLong(1);

    final AtomicReference<List<ResourceItem>> resourceRefs = new AtomicReference<>();

    final AtomicReference<List<ResourceTemplate>> resourceTemplateRefs = new AtomicReference<>();

    final AtomicReference<List<PromptItem>> promptRefs = new AtomicReference<>();

    public BaseMcpClient(Builder builder) {
        clientName = McpUtils.getOrDefault(builder.clientName, "aj-mcp");
        clientVersion = McpUtils.getOrDefault(builder.clientVersion, "1.0");
        protocolVersion = McpUtils.getOrDefault(builder.protocolVersion, "2024-11-05");

        transport = Objects.requireNonNull(builder.transport, "transport required");

        requestTimeout = McpUtils.getOrDefault(builder.toolExecutionTimeout, Duration.ofSeconds(60));
    }

    public void initialize() {
        transport.start(pendingOperations);

        long operationId = idGenerator.getAndIncrement();
//        InitializeRequest request = new InitializeRequest(operationId);
        InitializeRequest request = new InitializeRequest();
        request.setId(operationId);
        request.setParams(createInitializeParams());

        try {
            CompletableFuture<JsonNode> future = transport.initialize(request);
            JsonNode capabilities = future.get();
            log.info("MCP server capabilities: {}", capabilities.get("result"));
        } catch (ExecutionException e) {
            log.warn("ExecutionException when initializing MCP", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.warn("InterruptedException when initializing MCP", e);
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    /**
     * Create Initialize Params
     *
     * @return Initialize Params
     */
    private InitializeRequestParams createInitializeParams() {
        InitializeRequestParams params = new InitializeRequestParams();
        params.setProtocolVersion(protocolVersion);

        InitializeRequestParams.ClientInfo clientInfo = new InitializeRequestParams.ClientInfo();
        clientInfo.setName(clientName);
        clientInfo.setVersion(clientVersion);
        params.setClientInfo(clientInfo);

        InitializeRequestParams.Capabilities capabilities = new InitializeRequestParams.Capabilities();
        InitializeRequestParams.Capabilities.Roots roots = new InitializeRequestParams.Capabilities.Roots();
        roots.setListChanged(false); // TODO: listChanged is not supported yet
        capabilities.setRoots(roots);
        params.setCapabilities(capabilities);

        return params;
    }

    @Override
    public void checkHealth() {
        transport.checkHealth();
        long operationId = idGenerator.getAndIncrement();
//        PingRequest ping = new PingRequest(operationId);
        PingRequest ping = new PingRequest();
        ping.setId(operationId);

        try {
            CompletableFuture<JsonNode> resultFuture = transport.executeOperationWithResponse(ping);
            resultFuture.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    @Override
    public void close() {
        try {
            transport.close();
        } catch (Exception e) {
            log.warn("Cannot close MCP transport", e);
        }
    }

    public static class Builder {
        private McpTransport transport;
        private String clientName;
        private String clientVersion;
        private String protocolVersion;
        private Duration toolExecutionTimeout;

        public BaseMcpClient.Builder transport(McpTransport transport) {
            this.transport = transport;
            return this;
        }

        public BaseMcpClient.Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public BaseMcpClient.Builder clientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public BaseMcpClient.Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public BaseMcpClient.Builder toolExecutionTimeout(Duration toolExecutionTimeout) {
            this.toolExecutionTimeout = toolExecutionTimeout;
            return this;
        }

        public McpClient build() {
            return new McpClient(this);
        }
    }
}
