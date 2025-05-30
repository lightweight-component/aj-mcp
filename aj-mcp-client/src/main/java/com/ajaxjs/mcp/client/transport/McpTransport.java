package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    private Map<Long, CompletableFuture<JsonNode>> pendingRequests;

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

    public void setPendingRequests(Map<Long, CompletableFuture<JsonNode>> pendingRequests) {
        this.pendingRequests = pendingRequests;
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
        if (message.has(ID)) {
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
        } else if (message.has(METHOD) && message.get(METHOD).asText().equals("notifications/message")) {
            if (message.has(PARAMS))  // this is a log message
                log.info(message.get(PARAMS).asText());
            else
                log.warn("Received log message without params: {}", message);
        } else
            log.warn("Received unknown message: {}", message);
    }
}
