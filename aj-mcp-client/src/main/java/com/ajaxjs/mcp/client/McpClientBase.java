package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base Class for MCP Client, mainly doing the initialize job.
 */
@Slf4j
@SuperBuilder
public abstract class McpClientBase implements IMcpClient, McpConstant {
    McpTransport transport;

    /**
     * Sets the name that the client will use to identify itself to the MCP server in the initialization message.
     */
    @Builder.Default
    String clientName = "aj-mcp";

    /**
     * Sets the version string that the client will use to identify itself to the MCP server in the initialization message. The default value is "1.0".
     */
    @Builder.Default
    String clientVersion = "1.0";

    /**
     * Sets the protocol version that the client will advertise in the
     * initialization message. The default value right now is "2024-11-05", but will change over time in later versions.
     */
    @Builder.Default
    String protocolVersion = "2024-11-05";

    /**
     * Sets the timeout for tool execution. This value applies to each tool execution individually.
     * The default value is 60 seconds. A value of zero means no timeout.
     */
    @Builder.Default
    Duration requestTimeout = Duration.ofSeconds(60);

    final Map<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    final AtomicLong idGenerator = new AtomicLong(1);

    final AtomicReference<List<ResourceItem>> resourceRefs = new AtomicReference<>();

    final AtomicReference<List<ResourceTemplate>> resourceTemplateRefs = new AtomicReference<>();

    final AtomicReference<List<PromptItem>> promptRefs = new AtomicReference<>();

    @Override
    public void initialize() {
        transport.start(pendingRequests);
        long operationId = idGenerator.getAndIncrement();
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
            pendingRequests.remove(operationId);
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
        PingRequest ping = new PingRequest();
        ping.setId(operationId);

        try {
            CompletableFuture<JsonNode> resultFuture = transport.sendRequestWithResponse(ping);
            resultFuture.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(operationId);
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
}
