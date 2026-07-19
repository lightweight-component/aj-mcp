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
     * Sets the timeout for every request, including initialization and health checks.
     * The default value is 60 seconds. A value of zero means no timeout.
     */
    @Builder.Default
    Duration requestTimeout = Duration.ofSeconds(60);

    final Map<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    final AtomicLong idGenerator = new AtomicLong(1);

    final Map<Integer, List<ResourceItem>> resourceRefs = new ConcurrentHashMap<>();

    final Map<Integer, List<ResourceTemplate>> resourceTemplateRefs = new ConcurrentHashMap<>();

    final Map<Integer, List<PromptItem>> promptRefs = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        transport.start(pendingRequests);
        long operationId = idGenerator.getAndIncrement();
        InitializeRequest request = new InitializeRequest();
        request.setId(operationId);
        request.setParams(createInitializeParams());

        try {
            CompletableFuture<JsonNode> future = transport.initialize(request); // here is almost a synchronous call
            JsonNode capabilities = awaitResponse(future);
            log.info("MCP server capabilities: {}", capabilities.get("result"));
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out initializing MCP client after " + requestTimeout, e);
        } catch (ExecutionException e) {
            log.warn("ExecutionException when initializing MCP", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("InterruptedException when initializing MCP", e);
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(operationId);
        }
    }

    /**
     * Waits for an MCP response using the timeout policy shared by every client operation.
     * A zero duration means an unlimited wait.
     */
    protected JsonNode awaitResponse(CompletableFuture<JsonNode> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (requestTimeout.isNegative())
            throw new IllegalArgumentException("requestTimeout must not be negative");

        if (requestTimeout.isZero())
            return future.get();

        // CompletableFuture only accepts a numeric timeout. Preserve positive
        // sub-millisecond durations instead of accidentally turning them into zero.
        long timeoutMillis = Math.max(1L, requestTimeout.toMillis());
        return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
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
            awaitResponse(resultFuture);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException | TimeoutException e) {
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
