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
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Slf4j
public class ServerSse implements McpTransportSync {
    private McpServer server;

    public ServerSse(McpServer server) {
        this.server = server;
    }

    final Map<String, PrintWriter> connections = new ConcurrentHashMap<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile ScheduledExecutorService heartbeatExecutor;

    /**
     * Add a connection when the client connects
     *
     * @param clientId The client id.
     * @param writer   The output stream.
     */
    public synchronized void addConnections(String clientId, PrintWriter writer) {
        if (closed.get())
            throw new IllegalStateException("SSE server transport is closed");

        PrintWriter previous = connections.put(clientId, writer);
        if (previous != null && previous != writer)
            closeWriter(previous);
    }

    /**
     * Remove a connection when the client disconnects
     *
     * @param clientId The client id.
     */
    public void removeConnection(String clientId) {
        PrintWriter writer = connections.remove(clientId);
        if (writer != null)
            closeWriter(writer);
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
        synchronized (this) {
            if (closed.get())
                throw new IllegalStateException("SSE server transport is closed");
            if (heartbeatExecutor != null)
                return;

            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "aj-mcp-server-sse-heartbeat");
                thread.setDaemon(true);
                return thread;
            });
            heartbeatExecutor.scheduleAtFixedRate(() -> broadcast("heartbeat"), 0, 15, TimeUnit.SECONDS);
        }
    }

    // TODO Regularly clean up disconnected clients.
    // Detect client disconnects and clean up stale connections.
    // Use thread pools or reactive frameworks to handle many connections efficiently.
    // Use heartbeats (: heartbeat) to keep connections alive and prevent timeouts


    public static void output(PrintWriter writer, String data) {
        writeFrame(writer, "data: " + data + "\n\n");
    }

    private static void writeFrame(PrintWriter writer, String frame) {
        synchronized (writer) {
            writer.write(frame);
            writer.flush();
            if (writer.checkError())
                throw new IllegalStateException("SSE connection write failed");
        }
    }

    private static void closeWriter(PrintWriter writer) {
        synchronized (writer) {
            writer.close();
        }
    }

    public void returnMessage(String uuid, String data) {
        if (data == null)
            return;

        PrintWriter writer = connections.get(uuid);

        if (writer == null)
            throw new IllegalStateException("Connection id: " + uuid + " is not found.");

//        writer.write("id: " + 2 + "\n");
        try {
            writeFrame(writer, "event: message\ndata: " + data + "\n\n");
        } catch (RuntimeException e) {
            removeConnection(uuid);
            throw e;
        }
    }

    @Override
    public void start() {
        heartbeat();
    }

    @Override
    public String handle(String rawJson) {
        McpRequestRawInfo request = McpServerInitialize.jsonRpcValidate(rawJson); // 解析输入消息
        McpResponse mcpResponse = server.processMessage(request);

        return mcpResponse == null ? null : JsonUtils.toJson(mcpResponse);  // 处理消息并生成响应
    }

    @Override
    public void initialize() {

    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed.compareAndSet(false, true))
            return;

        ScheduledExecutorService executor;
        synchronized (this) {
            executor = heartbeatExecutor;
            heartbeatExecutor = null;
        }

        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS))
                    log.warn("SSE heartbeat executor did not stop within 2 seconds");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (String clientId : connections.keySet())
            removeConnection(clientId);
    }
}
