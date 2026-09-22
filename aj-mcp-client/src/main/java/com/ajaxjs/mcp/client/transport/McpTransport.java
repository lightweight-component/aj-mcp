package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.ProtocolVersion;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base contract for transports used by the MCP client.
 *
 * <p>A transport has two lifecycle phases: {@link #start(Map)} opens the
 * underlying channel and installs pending-request state, while
 * {@link #initialize(InitializeRequest)} performs MCP protocol negotiation.
 * Callers must complete initialization before sending ordinary requests.
 * Implementations must serialize writes appropriate to their framing and must
 * fail pending futures when the channel closes unexpectedly.</p>
 *
 * <p>The transport also dispatches server notifications and server-initiated
 * requests to handlers configured by the client. The latter must be answered
 * through the transport's native framing; transports that do not support that
 * direction may reject it explicitly.</p>
 */
@Slf4j
public abstract class McpTransport implements McpConstant, Closeable {
    /**
     * 创建连接到 MCP 服务器（如果需要，则运行服务器作为子进程）。
     * 此方法不发送“初始化”消息以协商功能、支持的协议版本等。
     * <p>
     * Creates a connection to the MCP server (runs the server as a subprocess if needed).
     * This does NOT yet send the "initialize" message to negotiate capabilities.
     *
     * @param pendingRequest A map of pending operations, where the key is the operation ID and the value is a CompletableFuture that will be completed when the operation is finished.
     */
    public abstract void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest);

    /**
     * 发送“初始化”消息以协商功能、支持的协议版本等。该方法在 “start”方法之后调用。
     * <p>
     * Sends the "initialize" message to the MCP server to negotiate
     * capabilities, supported protocol version etc. When this method
     * returns successfully, the transport is fully initialized and ready to
     * be used. This has to be called AFTER the "start" method.
     *
     * @param request 要发送的请求 The request to be sent.
     * @return 服务返回的响应（异步） The future response from the server.
     */
    public abstract CompletableFuture<JsonNode> initialize(InitializeRequest request);

    /**
     * Sends either message of the initialization handshake using the transport's
     * native framing. The returned future represents a response when {@code id}
     * is non-null and an accepted notification otherwise.
     *
     * @param initializeResponse      the future returned by the initialize request
     * @param initializedNotification supplies the future for the initialized notification
     * @return a future completed with the initialize response after the notification is accepted
     */
    protected CompletableFuture<JsonNode> completeInitialization(CompletableFuture<JsonNode> initializeResponse,
                                                                 Supplier<CompletableFuture<JsonNode>> initializedNotification) {
        // Keep the shared response so callers observe initialize's result after
        // the required notifications/initialized message is accepted.
        return initializeResponse.thenCompose(response -> {
            McpException.checkForErrors(response);
            JsonNode version = response.path(RESPONSE_RESULT).path("protocolVersion");
            if (!version.isTextual() || !supportedProtocolVersions.contains(version.textValue()))
                throw new IllegalStateException("Server selected unsupported protocol version: " + version);
            setNegotiatedProtocolVersion(version.textValue());
            return initializedNotification.get().thenApply(ignored -> response);
        });
    }

    /**
     * 发送请求到服务端，有响应返回。
     * <p>
     * Executes an operation that expects a response from the server.
     *
     * @param request 要发送的请求 The request to be sent.
     * @return 服务返回的响应（异步） The future response from the server.
     */
    public abstract CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request);

    /**
     * 发送不需要响应的请求。这时候消息的 id 字段应该是 null 的。
     * <p>
     * Sends a message that does not expect a response from the server. The 'id' field of the message should be null.
     *
     * @param request 要发送的请求 The request to be sent.
     */
    public abstract void sendRequestWithoutResponse(McpRequest request);

    /**
     * Sends a JSON-RPC response generated for a server-initiated request.
     *
     * @param message the JSON-RPC response message to send.
     */
    protected void sendJson(JsonNode message) {
        throw new UnsupportedOperationException("This transport cannot answer server-initiated requests");
    }

    /**
     * PING 检查
     * <p>
     * Performs transport-specific health checks, if applicable. This is called
     * by `McpClient.checkHealth()` as the first check before performing a check
     * by sending a 'ping' over the MCP protocol. The purpose is that the
     * transport may have some specific and faster ways to detect that it is broken,
     * like for example, the STDIO transport can fail the check if it detects
     * that the server subprocess isn't alive anymore.
     */
    public abstract void checkHealth();

    /**
     * Session-local request table used to correlate numeric JSON-RPC responses.
     * It must not be shared by unrelated client sessions.
     */
    @Setter
    private Map<Long, CompletableFuture<JsonNode>> pendingRequests;
    /**
     * Callback for server notifications, receiving the complete JSON message.
     */
    private Consumer<JsonNode> notificationHandler;
    /**
     * Callback for server-initiated requests, receiving the complete JSON message.
     */
    private Function<JsonNode, JsonNode> serverRequestHandler;
    /**
     * True only after the initialize response and initialized notification succeed.
     */
    private volatile boolean initialized;

    /**
     * Protocol revision selected by the server during initialization.
     */
    @Getter
    @Setter
    private volatile String negotiatedProtocolVersion;

    /**
     * Protocol revisions accepted before acknowledging initialization. The
     * server's selected value must occur in this list.
     */
    @Setter
    private List<String> supportedProtocolVersions = ProtocolVersion.supportedVersions();

    /**
     * Installs callbacks for messages initiated by the server.
     *
     * @param notificationHandler  receives JSON-RPC notifications; may be null
     * @param serverRequestHandler receives server requests and returns their JSON result; may be null
     */
    public void setMessageHandlers(Consumer<JsonNode> notificationHandler,
                                   Function<JsonNode, JsonNode> serverRequestHandler) {
        this.notificationHandler = notificationHandler;
        this.serverRequestHandler = serverRequestHandler;
    }

    /**
     * Marks the transport ready for ordinary MCP requests.
     * This must be called only after successful initialization.
     */
    public void markInitialized() {
        initialized = true;
    }

    /**
     * Enforces the post-handshake lifecycle boundary for request operations.
     *
     * @throws IllegalStateException if initialization has not completed
     */
    protected void requireInitialized() {
        if (!initialized)
            throw new IllegalStateException("MCP client is not initialized");
    }

    /**
     * Executes the numeric id operation.
     *
     * @param id the id value.
     * @return the result of the numeric id operation.
     */
    protected static Long numericId(Object id) {
        if (!(id instanceof Number))
            throw new IllegalArgumentException("Client-generated request id must be numeric: " + id);

        return ((Number) id).longValue();
    }

    /**
     * 如果一个请求需要响应，那么在发送请求之前，必须调用此方法，将请求的 id 保存起来，以便可以对应到响应。
     * <p>
     * A transport also has to call "saveRequest" when before starting a request that requires a response
     * to register its ID in the map of pending requests.
     *
     * @param id     The request id
     * @param future The request going to send
     */
    public void saveRequest(Long id, CompletableFuture<JsonNode> future) {
        if (pendingRequests == null)
            throw new UnsupportedOperationException("MCP Client is NOT initialized");

        pendingRequests.put(id, future);
        future.whenComplete((result, failure) -> pendingRequests.remove(id, future));
    }

    /**
     * Completes and removes every outstanding request when the transport can no longer
     * deliver responses, for example after a connection failure or shutdown.
     *
     * @param cause the transport failure reported to request callers
     */
    public void failPendingRequests(Throwable cause) {
        if (pendingRequests == null)
            return;

        pendingRequests.forEach((id, future) -> {
            if (pendingRequests.remove(id, future))
                future.completeExceptionally(cause);
        });
    }

    /**
     * 解析来自 MCP 服务器的 JSON 报文。
     * 首先获取 id 字段，用于确定响应消息所对应的请求。另外还针对 ping 以及 notifications/message 方法的响应进行处理。
     * <p>
     * Handles incoming JSON messages from the MCP server.
     * This method processes different types of messages based on their content.
     * It checks for the presence of an "id" field to determine if it's a response to a pending request, and handles "ping" method messages specifically.
     * Additionally, it processes log messages under the "notifications/message" method.
     *
     * @param message 要解析的 JSON 报文，是为 Jackson 的 JsonNode 对象。The JSON message to be handled, represented as a JsonNode object.
     */
    public void handle(JsonNode message) {
        // A message containing both method and id is a server-initiated request,
        // not a response to one of the client's pending operations.
        if (message.has(METHOD) && message.has(ID)) {
            ObjectNode response = JsonUtils.createObjectNode();
            response.put("jsonrpc", BaseJsonRpcMessage.VERSION);
            response.set(ID, message.get(ID));

            // Ping is a protocol operation, independent of application capabilities.
            if (Methods.PING.equals(message.path(METHOD).asText()))
                response.putObject(RESPONSE_RESULT);
            else if (serverRequestHandler == null)
                response.putObject("error").put("code", -32601).put("message",
                        "No client handler for method " + message.get(METHOD).asText());
            else {
                try {
                    JsonNode result = serverRequestHandler.apply(message);

                    if (result != null)
                        response.set(RESPONSE_RESULT, result);
                    else
                        response.putObject("error").put("code", -32601).put("message",
                                "No client handler for method " + message.get(METHOD).asText());
                } catch (RuntimeException e) {
                    log.warn("Client request handler failed for {}", message.get(METHOD).asText(), e);
                    response.putObject("error").put("code", -32603)
                            .put("message", "Client request handler failed");
                }
            }
            sendJson(response);
        } else if (message.has(ID)) {
            long messageId = message.get(ID).asLong();
            CompletableFuture<JsonNode> op = pendingRequests.remove(messageId);

            if (op != null)
                op.complete(message);
            else {
                log.warn("Received response for unknown message id: {}", messageId);
            }
        } else if (message.has(METHOD)) {
            if (notificationHandler != null)
                notificationHandler.accept(message);
            else if (message.get(METHOD).asText().equals("notifications/message"))
                log.info("{}", message.get(PARAMS));
            else
                log.warn("Received notification without a handler: {}", message.get(METHOD).asText());
        } else
            log.warn("Received unknown message: {}", message);
    }
}
