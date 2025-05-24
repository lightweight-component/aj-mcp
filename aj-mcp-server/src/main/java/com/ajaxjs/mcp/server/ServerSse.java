package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.transport.McpTransportSync;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class ServerSse implements McpTransportSync {
    private McpServer server;

    public ServerSse(McpServer server) {
        this.server = server;
    }

    final Map<String, PrintWriter> connections = new ConcurrentHashMap<>();

    /**
     * Add a connection when the client connects
     *
     * @param clientId The client id.
     * @param writer   The output stream.
     */
    public void addConnections(String clientId, PrintWriter writer) {
        connections.put(clientId, writer);
    }

    /**
     * Remove a connection when the client disconnects
     *
     * @param clientId The client id.
     */
    public void removeConnection(String clientId) {
        connections.remove(clientId);
    }

    /**
     * Broadcast a message to all connected clients.
     *
     * @param data The data to send
     */
    public void broadcast(String data) {
        for (Map.Entry<String, PrintWriter> entry : connections.entrySet()) {
            try {
                output(entry.getValue(), data);
            } catch (Exception e) {
                // Handle errors (e.g., remove disconnected clients)
                log.warn("Error sending to client {}: {}", entry.getKey(), e.getMessage());
                removeConnection(entry.getKey());
            }
        }
    }

    /**
     * Avoid blocking threads for each client connection.
     * Use a thread pool or an executor framework to handle connections efficiently. For large numbers of clients, consider limiting the thread count.
     */
    public void heartbeat() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        executor.scheduleAtFixedRate(() -> broadcast("heartbeat"), 0, 15, TimeUnit.SECONDS);
    }

    // TODO Regularly clean up disconnected clients.
    // Detect client disconnects and clean up stale connections.
    // Use thread pools or reactive frameworks to handle many connections efficiently.
    // Use heartbeats (: heartbeat) to keep connections alive and prevent timeouts


    public static void output(PrintWriter writer, String data) {
        writer.write("data: " + data + "\n\n");
        writer.flush();
    }

    public void returnMessage(String uuid, String data) {
        PrintWriter writer = connections.get(uuid);

        if (writer == null)
            throw new IllegalStateException("Connection id: " + uuid + " is not found.");

//        writer.write("id: " + 2 + "\n");
        writer.write("event: message\n");
        output(writer, data);
    }

    @Override
    public void start() {
    }

    @Override
    public String handle(String rawJson) {
        McpRequestRawInfo request = McpServerInitialize.jsonRpcValidate(rawJson); // 解析输入消息
        McpResponse mcpResponse = server.processMessage(request);

        return JsonUtils.toJson(mcpResponse);  // 处理消息并生成响应
    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() throws IOException {

    }
}
