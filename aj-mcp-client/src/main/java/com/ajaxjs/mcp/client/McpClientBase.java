package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.ProtocolVersion;
import com.ajaxjs.mcp.protocol.client.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

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
     * Revisions this client can accept if the server selects a fallback.
     */
    @Builder.Default
    List<String> supportedProtocolVersions = ProtocolVersion.supportedVersions();

    private volatile String negotiatedProtocolVersion;

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

    final Map<String, Consumer<JsonNode>> notificationHandlers = new ConcurrentHashMap<>();

    final Map<String, Function<JsonNode, JsonNode>> serverRequestHandlers = new ConcurrentHashMap<>();

    volatile List<Root> roots;

    volatile boolean rootsListChanged;

    @Override
    public void initialize() {
        transport.setMessageHandlers(this::handleNotification, this::handleServerRequest);
        transport.start(pendingRequests);
        long operationId = idGenerator.getAndIncrement();
        InitializeRequest request = new InitializeRequest();
        request.setId(operationId);
        request.setParams(createInitializeParams());

        try {
            CompletableFuture<JsonNode> future = transport.initialize(request); // here is almost a synchronous call
            JsonNode capabilities = awaitResponse(future);
            JsonNode negotiatedVersion = capabilities.path(RESPONSE_RESULT).path("protocolVersion");

            if (!supportedProtocolVersions.contains(negotiatedVersion.asText()))
                throw new IllegalStateException("Server selected unsupported protocol version: " + negotiatedVersion.asText());

            negotiatedProtocolVersion = negotiatedVersion.asText();
            transport.setNegotiatedProtocolVersion(negotiatedProtocolVersion);
            transport.markInitialized();
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

    @Override
    public void onNotification(String method, Consumer<JsonNode> handler) {
        notificationHandlers.put(method, handler);
    }

    @Override
    public void onServerRequest(String method, Function<JsonNode, JsonNode> handler) {
        serverRequestHandlers.put(method, handler);
    }

    @Override
    public void setRoots(List<Root> roots, boolean notifyChanges) {
        this.roots = java.util.Collections.unmodifiableList(new ArrayList<>(roots));
        this.rootsListChanged = notifyChanges;

        onServerRequest(Methods.ROOTS_LIST, ignored -> {
            com.fasterxml.jackson.databind.node.ObjectNode result = JsonUtils.OBJECT_MAPPER.createObjectNode();
            result.set("roots", JsonUtils.OBJECT_MAPPER.valueToTree(this.roots));
            return result;
        });
    }

    @Override
    public void notifyRootsChanged() {
        if (!rootsListChanged)
            throw new IllegalStateException("roots listChanged capability was not enabled");

        McpRequest notification = new McpRequest();
        notification.setMethod(Methods.ROOTS_LIST_CHANGED_NOTIFICATION);
        transport.sendRequestWithoutResponse(notification);
    }

    @Override
    public void setSamplingHandler(Function<SamplingCreateMessageParams, SamplingCreateMessageResult> handler) {
        onServerRequest(Methods.SAMPLING_CREATE_MESSAGE, params -> JsonUtils.OBJECT_MAPPER.valueToTree(
                handler.apply(JsonUtils.OBJECT_MAPPER.convertValue(params, SamplingCreateMessageParams.class))));
    }

    @Override
    public void setElicitationHandler(Function<ElicitRequestParams, ElicitResult> handler) {
        onServerRequest(Methods.ELICITATION_CREATE, params -> JsonUtils.OBJECT_MAPPER.valueToTree(
                handler.apply(JsonUtils.OBJECT_MAPPER.convertValue(params, ElicitRequestParams.class))));
    }

    @Override
    public String getNegotiatedProtocolVersion() {
        return negotiatedProtocolVersion;
    }

    private void handleNotification(JsonNode message) {
        String method = message.get(METHOD).asText();
        // List-change notifications invalidate all pages because an insertion can
        // shift every cursor/page boundary, not only the first cached page.

        if (Methods.TOOLS_LIST_CHANGED_NOTIFICATION.equals(method)) {
            // Tool lists are currently uncached.
        } else if (Methods.RESOURCE_LIST_CHANGED_NOTIFICATION.equals(method)) {
            resourceRefs.clear();
            resourceTemplateRefs.clear();
        } else if (Methods.PROMPTS_LIST_CHANGED_NOTIFICATION.equals(method))
            promptRefs.clear();

        Consumer<JsonNode> handler = notificationHandlers.get(method);

        if (handler != null) {
            try {
                handler.accept(message.get(PARAMS));
            } catch (RuntimeException e) {
                log.warn("Notification handler failed for {}", method, e);
            }
        } else if ("notifications/message".equals(method))
            log.info("MCP log message: {}", message.get(PARAMS));
    }

    private JsonNode handleServerRequest(JsonNode message) {
        Function<JsonNode, JsonNode> handler = serverRequestHandlers.get(message.get(METHOD).asText());

        return handler == null ? null : handler.apply(message.get(PARAMS));
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

        if (serverRequestHandlers.containsKey(Methods.ROOTS_LIST)) {
            InitializeRequestParams.Capabilities.Roots roots = new InitializeRequestParams.Capabilities.Roots();
            roots.setListChanged(rootsListChanged);
            capabilities.setRoots(roots);
        }

        if (serverRequestHandlers.containsKey(Methods.SAMPLING_CREATE_MESSAGE))
            capabilities.setSampling(new InitializeRequestParams.Capabilities.Sampling());

        if (serverRequestHandlers.containsKey(Methods.ELICITATION_CREATE)) {
            if (!ProtocolVersion.from(protocolVersion).supportsElicitation())
                throw new IllegalStateException("Elicitation requires MCP 2025-06-18 or newer");
            capabilities.setElicitation(new InitializeRequestParams.Capabilities.Elicitation());
        }

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
