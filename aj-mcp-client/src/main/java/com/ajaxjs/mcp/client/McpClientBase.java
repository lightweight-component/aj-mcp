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
 * Shared implementation of the synchronous MCP client API.
 *
 * <p>This class owns request identifiers, pending-response tracking, protocol
 * negotiation, pagination caches, and callbacks for server-originated messages.
 * Subclasses only need to provide feature operations and a configured
 * {@link McpTransport}. Instances represent one MCP session and should not be
 * shared across independent transports or servers.</p>
 *
 * <p>Operations use a bounded request wait. A null or zero timeout is normalized
 * to the default of 60 seconds, rather than creating an accidental infinite
 * wait. Initialization failures close the transport and clean up pending state.</p>
 */
@Slf4j
@SuperBuilder
public abstract class McpClientBase implements IMcpClient, McpConstant {
    /**
     * Transport used for the lifetime of this client session.
     *
     * <p>The transport must be configured before {@link #initialize()} and
     * should not be replaced while requests are in flight.</p>
     */
    McpTransport transport;

    /**
     * Sets the name that the client will use to identify itself to the MCP server in the initialization message.
     */
    @Builder.Default
    String clientName = "aj-mcp";

    /**
     * Optional display label, separate from the stable clientName identifier.
     */
    String clientTitle;

    /**
     * Sets the version string that the client will use to identify itself to the MCP server in the initialization message. The default value is "1.0".
     */
    @Builder.Default
    String clientVersion = "1.0";

    /**
     * Protocol revision initially advertised in the initialization request.
     *
     * <p>The server may select a different value from
     * {@link #supportedProtocolVersions}. Revision-specific capabilities are
     * validated after negotiation; setting a newer revision does not make an
     * older server support it.</p>
     */
    @Builder.Default
    String protocolVersion = "2024-11-05";

    /**
     * Protocol revisions this client accepts in the server's initialize result.
     *
     * <p>The list is copied into the transport during initialization. It should
     * contain only revisions understood by the application; selecting another
     * revision causes initialization to fail.</p>
     */
    @Builder.Default
    List<String> supportedProtocolVersions = ProtocolVersion.supportedVersions();

    /**
     * Holds the negotiated protocol version value.
     */
    private volatile String negotiatedProtocolVersion;

    /**
     * Maximum wait for each synchronous request, including initialization and
     * health checks.
     *
     * <p>Null or zero uses a 60-second default. Negative values are rejected.
     * On timeout the underlying future is cancelled and its pending entry is
     * removed.</p>
     */
    @Builder.Default
    Duration requestTimeout = Duration.ofSeconds(60);

    /**
     * Per-session map from client request id to response future.
     * Entries are removed after response, failure, timeout, or close.
     */
    final Map<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Holds the id generator value.
     */
    final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Cached resource pages keyed by page number; invalidated on resource-list changes.
     */
    final Map<Integer, List<ResourceItem>> resourceRefs = new ConcurrentHashMap<>();

    /**
     * Cached resource-template pages keyed by page number; invalidated on resource-list changes.
     */
    final Map<Integer, List<ResourceTemplate>> resourceTemplateRefs = new ConcurrentHashMap<>();

    /**
     * Cached prompt pages keyed by page number; invalidated on prompt-list changes.
     */
    final Map<Integer, List<PromptItem>> promptRefs = new ConcurrentHashMap<>();

    /**
     * Holds the notification handlers value.
     */
    final Map<String, Consumer<JsonNode>> notificationHandlers = new ConcurrentHashMap<>();

    /**
     * Holds the server request handlers value.
     */
    final Map<String, Function<JsonNode, JsonNode>> serverRequestHandlers = new ConcurrentHashMap<>();

    /**
     * Holds the roots value.
     */
    volatile List<Root> roots;

    /**
     * Holds the roots list changed value.
     */
    volatile boolean rootsListChanged;

    /**
     * Starts the transport, sends initialize, validates the selected protocol
     * revision, and marks the transport ready for feature requests.
     *
     * <p>Handlers are installed before startup so server-originated requests
     * received during or immediately after initialization can be answered. The
     * pending initialize entry is removed in all outcomes; unsuccessful
     * initialization closes the transport.</p>
     */
    @Override
    public void initialize() {
        transport.setMessageHandlers(this::handleNotification, this::handleServerRequest);
        transport.setSupportedProtocolVersions(new ArrayList<>(supportedProtocolVersions));
        long operationId = idGenerator.getAndIncrement();
        InitializeRequest request = new InitializeRequest();
        request.setId(operationId);
        request.setParams(createInitializeParams());

        try {
            transport.start(pendingRequests);
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
            if (negotiatedProtocolVersion == null)
                close();
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
            com.fasterxml.jackson.databind.node.ObjectNode result = JsonUtils.createObjectNode();
            result.set("roots", JsonUtils.valueToTree(this.roots));
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
        onServerRequest(Methods.SAMPLING_CREATE_MESSAGE, params -> JsonUtils.valueToTree(
                handler.apply(JsonUtils.convertValue(params, SamplingCreateMessageParams.class))));
    }

    @Override
    public void setElicitationHandler(Function<ElicitRequestParams, ElicitResult> handler) {
        onServerRequest(Methods.ELICITATION_CREATE, params -> JsonUtils.valueToTree(
                handler.apply(JsonUtils.convertValue(params, ElicitRequestParams.class))));
    }

    @Override
    public String getNegotiatedProtocolVersion() {
        return negotiatedProtocolVersion;
    }

    /**
     * Executes the handle notification operation.
     *
     * @param message the message value.
     */
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

    /**
     * Executes the handle server request operation.
     *
     * @param message the message value.
     * @return the result of the handle server request operation.
     */
    private JsonNode handleServerRequest(JsonNode message) {
        Function<JsonNode, JsonNode> handler = serverRequestHandlers.get(message.get(METHOD).asText());

        return handler == null ? null : handler.apply(message.get(PARAMS));
    }

    /**
     * Waits for an MCP response using the timeout policy shared by every client operation.
     * A zero duration means an unlimited wait.
     *
     * @param future the pending response future.
     * @return the received JSON-RPC response.
     * @throws InterruptedException if the waiting thread is interrupted.
     * @throws ExecutionException   if the request completes exceptionally.
     * @throws TimeoutException     if the configured request timeout expires.
     */
    protected JsonNode awaitResponse(CompletableFuture<JsonNode> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        Duration effectiveTimeout = requestTimeout == null || requestTimeout.isZero()
                ? Duration.ofSeconds(60) : requestTimeout;
        if (effectiveTimeout.isNegative())
            throw new IllegalArgumentException("requestTimeout must not be negative");

        // CompletableFuture only accepts a numeric timeout. Preserve positive
        // sub-millisecond durations instead of accidentally turning them into zero.
        long timeoutMillis = Math.max(1L, effectiveTimeout.toMillis());

        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException e) {
            future.cancel(true);
            throw e;
        }
    }

    /**
     * Sends a request and applies the common synchronous client lifecycle. The
     * transport removes successful responses itself; the finally block also
     * covers timeouts and send failures, preventing stale pending entries.
     *
     * @param request the request to send
     * @return the response returned by the server
     */
    protected JsonNode executeRequest(McpRequest request) {
        try {
            return awaitResponse(transport.sendRequestWithResponse(request));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(request.getId());
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
        if (ProtocolVersion.from(protocolVersion).supportsTitles())
            clientInfo.setTitle(clientTitle);
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

        executeRequest(ping);
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
