package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MCP 客户端传输接口
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
     * Holds the pending requests value.
     */
    @Setter
    private Map<Long, CompletableFuture<JsonNode>> pendingRequests;
    /**
     * Holds the notification handler value.
     */
    private Consumer<JsonNode> notificationHandler;
    /**
     * Holds the server request handler value.
     */
    private Function<JsonNode, JsonNode> serverRequestHandler;
    /**
     * Holds the initialized value.
     */
    private volatile boolean initialized;

    /**
     * Holds the negotiated protocol version value.
     */
    @Getter
    @Setter
    private volatile String negotiatedProtocolVersion;

    /**
     * Executes the set message handlers operation.
     *
     * @param notificationHandler  the notification handler value.
     * @param serverRequestHandler the server request handler value.
     */
    public void setMessageHandlers(Consumer<JsonNode> notificationHandler,
                                   Function<JsonNode, JsonNode> serverRequestHandler) {
        this.notificationHandler = notificationHandler;
        this.serverRequestHandler = serverRequestHandler;
    }

    /**
     * Executes the mark initialized operation.
     */
    public void markInitialized() {
        initialized = true;
    }

    /**
     * Executes the require initialized operation.
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

            if (serverRequestHandler == null)
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
                if (message.has(METHOD)) {
                    String method = message.get(METHOD).asText();

                    if (method.equals("ping")) {
                        PingRequest req = new PingRequest();
                        req.setId(messageId);
                        sendRequestWithoutResponse(req);
                        return;
                    }
                }

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
