package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.transport.McpTransportSync;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents server sse.
 */
@Slf4j
public class ServerSse implements McpTransportSync {
    /**
     * Holds the server value.
     */
    private final McpServer server;

    /**
     * Creates a new server sse.
     *
     * @param server the server value.
     */
    public ServerSse(McpServer server) {
        this.server = Objects.requireNonNull(server, "server is required");
    }

    /**
     * Holds the connections value.
     */
    final Map<String, SseSession> connections = new ConcurrentHashMap<>();

    /**
     * Holds the started value.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Holds the closed value.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Holds the heartbeat executor value.
     */
    private volatile ScheduledExecutorService heartbeatExecutor;

    /**
     * Add a connection when the client connects
     *
     * @param clientId The client id.
     * @param writer   The output stream.
     */
    public synchronized void addConnections(String clientId, PrintWriter writer) {
        registerSession(clientId, writer);
    }

    /**
     * Registers a client and atomically sends the endpoint event required by the MCP SSE transport.
     *
     * @param clientId     the logical client session identifier.
     * @param writer       the response writer for the SSE stream.
     * @param endpointPath the relative POST endpoint advertised to the client.
     */
    public void openSession(String clientId, PrintWriter writer, String endpointPath) {
        if (endpointPath == null || endpointPath.trim().isEmpty())
            throw new IllegalArgumentException("endpointPath is required");

        SseSession session = registerSession(clientId, writer);

        try {
            session.sendFrame("event: endpoint\ndata: " + endpointPath + "\n\n");
        } catch (RuntimeException e) {
            removeConnection(clientId, session);
            throw e;
        }
    }

    /**
     * Executes the register session operation.
     *
     * @param clientId the client id value.
     * @param writer   the writer value.
     * @return the result of the register session operation.
     */
    private synchronized SseSession registerSession(String clientId, PrintWriter writer) {
        if (closed.get())
            throw new IllegalStateException("SSE server transport is closed");

        if (clientId == null || clientId.trim().isEmpty())
            throw new IllegalArgumentException("clientId is required");

        if (writer == null)
            throw new IllegalArgumentException("writer is required");

        SseSession session = new SseSession(writer);
        SseSession previous = connections.put(clientId, session);

        if (previous != null)
            previous.close();

        return session;
    }

    /**
     * Remove a connection when the client disconnects
     *
     * @param clientId The client id.
     */
    public void removeConnection(String clientId) {
        SseSession session = connections.remove(clientId);

        if (session != null)
            session.close();

        server.removeSession(clientId);
    }

    /**
     * Executes the remove connection operation.
     *
     * @param clientId the client id value.
     * @param session  the session value.
     */
    private void removeConnection(String clientId, SseSession session) {
        if (connections.remove(clientId, session)) {
            session.close();
            server.removeSession(clientId);
        }
    }

    /**
     * Executes the is session open operation.
     *
     * @param clientId the client id value.
     * @return the result of the is session open operation.
     */
    public boolean isSessionOpen(String clientId) {
        SseSession session = connections.get(clientId);
        return session != null && !session.isClosed();
    }

    /**
     * Executes the get session count operation.
     *
     * @return the result of the get session count operation.
     */
    public int getSessionCount() {
        return connections.size();
    }

    /**
     * Executes the is started operation.
     *
     * @return the result of the is started operation.
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * Executes the is closed operation.
     *
     * @return the result of the is closed operation.
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Executes the is heartbeat running operation.
     *
     * @return the result of the is heartbeat running operation.
     */
    public boolean isHeartbeatRunning() {
        ScheduledExecutorService executor = heartbeatExecutor;
        return executor != null && !executor.isShutdown();
    }

    /**
     * Broadcasts administrative data to all clients. Request responses must use
     * {@link #handle(String, String)} or {@link #returnMessage(String, String)}.
     *
     * @param data The data to send
     * @deprecated MCP request responses are session-scoped; retain only for legacy
     * administrative broadcasts.
     */
    @Deprecated
    @Override
    public void broadcast(String data) {
        for (Map.Entry<String, SseSession> entry : connections.entrySet()) {
            try {
                entry.getValue().sendData(data);
            } catch (Exception e) {
                // Handle errors (e.g., remove disconnected clients)
                log.warn("Error sending to client {}: {}", entry.getKey(), e.getMessage());
                removeConnection(entry.getKey(), entry.getValue());
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
            heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, 0, 15, TimeUnit.SECONDS);
        }
    }

    /**
     * Executes the send heartbeats operation.
     */
    private void sendHeartbeats() {
        for (Map.Entry<String, SseSession> entry : connections.entrySet()) {
            try {
                entry.getValue().sendFrame(": heartbeat\n\n");
            } catch (RuntimeException e) {
                log.debug("Removing disconnected SSE client {}", entry.getKey());
                removeConnection(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Executes the output operation.
     *
     * @param writer the writer value.
     * @param data   the data value.
     */
    public static void output(PrintWriter writer, String data) {
        writeFrame(writer, "data: " + data + "\n\n");
    }

    /**
     * Executes the write frame operation.
     *
     * @param writer the writer value.
     * @param frame  the frame value.
     */
    private static void writeFrame(PrintWriter writer, String frame) {
        synchronized (writer) {
            writer.write(frame);
            writer.flush();

            if (writer.checkError())
                throw new IllegalStateException("SSE connection write failed");
        }
    }

    /**
     * Executes the return message operation.
     *
     * @param uuid the uuid value.
     * @param data the data value.
     */
    public void returnMessage(String uuid, String data) {
        if (data == null)
            return;

        SseSession session = connections.get(uuid);

        if (session == null)
            throw new IllegalStateException("Connection id: " + uuid + " is not found.");

        try {
            session.sendFrame("event: message\ndata: " + data + "\n\n");
        } catch (RuntimeException e) {
            removeConnection(uuid, session);
            throw e;
        }
    }

    /**
     * Processes a request and sends its response only to the originating session.
     *
     * @param clientId the originating client session identifier.
     * @param rawJson  the JSON-RPC request payload.
     */
    public void handle(String clientId, String rawJson) {
        if (server.acceptClientResponse(clientId, rawJson))
            return;

        server.bindSession(clientId);

        try {
            returnMessage(clientId, handle(rawJson));
        } finally {
            server.clearSession();
        }
    }

    @Override
    public void start() {
        if (closed.get())
            throw new IllegalStateException("SSE server transport is closed");
        if (!started.compareAndSet(false, true))
            return;
        heartbeat();
    }

    @Override
    public String handle(String rawJson) {
        if (server.acceptClientResponse("default", rawJson))
            return null;

        McpRequestRawInfo request = McpServerInitialize.jsonRpcValidate(rawJson); // 解析输入消息
        McpResponse mcpResponse;

        try {
            mcpResponse = server.processMessage(request);
        } catch (JsonRpcErrorException e) {
            if (request.getId() == null)
                return null;
            throw e;
        }

        return mcpResponse == null ? null : JsonUtils.toJson(mcpResponse);  // 处理消息并生成响应
    }

    @Override
    public void initialize() {
        start();
    }

    @Override
    public void send(String sessionId, String json) {
        returnMessage(sessionId, json);
    }

    @Override
    public synchronized void close() {
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

    /**
     * Represents sse session.
     */
    static final class SseSession {
        /**
         * Holds the writer value.
         */
        private final PrintWriter writer;
        /**
         * Holds the closed value.
         */
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * Creates a new sse session.
         *
         * @param writer the writer value.
         */
        SseSession(PrintWriter writer) {
            this.writer = writer;
        }

        /**
         * Executes the send data operation.
         *
         * @param data the data value.
         */
        void sendData(String data) {
            sendFrame("data: " + data + "\n\n");
        }

        /**
         * Executes the send frame operation.
         *
         * @param frame the frame value.
         */
        void sendFrame(String frame) {
            synchronized (writer) {
                if (closed.get())
                    throw new IllegalStateException("SSE session is closed");
                writer.write(frame);
                writer.flush();
                if (writer.checkError())
                    throw new IllegalStateException("SSE connection write failed");
            }
        }

        /**
         * Executes the is closed operation.
         *
         * @return the result of the is closed operation.
         */
        boolean isClosed() {
            return closed.get();
        }

        /**
         * Executes the close operation.
         */
        void close() {
            synchronized (writer) {
                if (closed.compareAndSet(false, true))
                    writer.close();
            }
        }
    }
}
