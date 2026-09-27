package com.ajaxjs.mcp2026.client;

import com.ajaxjs.mcp2026.client.model.Exchange;
import com.ajaxjs.mcp2026.core.*;
import com.ajaxjs.util.httpremote.Request;
import com.ajaxjs.util.httpremote.model.HttpMethod;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One POST/response stream per request. Uses AJ HTTP initialization, without payload logging or redirects.
 */
public final class HttpClientTransport implements ClientTransport {
    /**
     * Validated absolute HTTP(S) endpoint used for every request.
     */
    private final String endpoint;
    /**
     * Immutable caller-supplied headers after framing and MCP-reserved headers are rejected.
     */
    private final Map<String, String> headers;
    /**
     * Positive connect and read timeout applied to each HTTP exchange, in milliseconds.
     */
    private final int ioTimeoutMs;
    /**
     * Strict JSON-RPC codec used for request bodies and JSON/SSE responses.
     */
    private final McpCodec codec = new McpCodec();
    /**
     * Bounded executor that owns blocking HTTP I/O.
     */
    private final ExecutorService io;
    /**
     * Separate executor used to disconnect connections without blocking cancellation callers.
     */
    private final ExecutorService cleanup;
    /**
     * Active exchanges keyed by request ID for response correlation and cancellation.
     */
    private final ConcurrentMap<RequestId, Exchange> active = new ConcurrentHashMap<>();
    /**
     * Latest validated tool descriptors needed to mirror declared HTTP parameter headers.
     */
    private final ConcurrentMap<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    /**
     * Idempotent shutdown state that prevents new exchanges.
     */
    private final AtomicBoolean closed = new AtomicBoolean();
    /**
     * Single listener installed before requests may be sent.
     */
    private Listener listener;

    /**
     * Creates an HTTP transport with no custom headers, a 15-second I/O bound, and up to 16 concurrent requests.
     *
     * @param endpoint absolute HTTP or HTTPS MCP endpoint
     */
    public HttpClientTransport(String endpoint) {
        this(endpoint, Collections.emptyMap(), 15000, 16);
    }

    /**
     * Creates an HTTP transport that uses one POST response stream per request.
     * Custom headers cannot override protocol-controlled or connection-sensitive headers; redirects are refused.
     *
     * @param endpoint    absolute HTTP or HTTPS endpoint without user info or fragment
     * @param headers     additional safe headers, excluding MCP and HTTP framing headers
     * @param ioTimeoutMs positive connect and read timeout for each HTTP exchange
     * @param concurrency maximum simultaneous HTTP exchanges
     */
    public HttpClientTransport(String endpoint, Map<String, String> headers, int ioTimeoutMs, int concurrency) {
        URI uri = URI.create(endpoint);

        if (!Arrays.asList("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("An absolute HTTP(S) endpoint without userinfo/fragment is required");

        if (ioTimeoutMs <= 0 || concurrency < 1)
            throw new IllegalArgumentException("Invalid I/O bounds");

        this.endpoint = endpoint;
        this.ioTimeoutMs = ioTimeoutMs;
        Map<String, String> copy = new LinkedHashMap<>();
        headers.forEach((k, v) -> {
            if (!k.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+") || v == null || v.indexOf('\r') >= 0 || v.indexOf('\n') >= 0
                    || k.toLowerCase(Locale.ROOT).startsWith("mcp-")
                    || Arrays.asList("host", "content-length", "transfer-encoding", "content-type", "accept", "connection").contains(k.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Unsafe/reserved custom header");

            copy.put(k, v);
        });

        this.headers = Collections.unmodifiableMap(copy);
        io = new ThreadPoolExecutor(concurrency, concurrency, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(128),
                McpClient.daemon("mcp-http-client"), new ThreadPoolExecutor.AbortPolicy());
        cleanup = new ThreadPoolExecutor(concurrency, concurrency, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(concurrency + 128),
                McpClient.daemon("mcp-http-close"), new ThreadPoolExecutor.AbortPolicy());
    }

    public boolean isHttp() {
        return true;
    }

    /**
     * Stores a validated descriptor so later {@code tools/call} requests can mirror declared HTTP parameter headers.
     *
     * @param tool tool descriptor obtained from a trusted {@code tools/list} response
     */
    public void tool(ToolDefinition tool) {
        HttpMetadata.validateTool(tool);
        tools.put(tool.getName(), tool);
    }

    /**
     * Installs the one listener used by all concurrent HTTP exchange workers.
     *
     * @param listener recipient for correlated messages and local exchange failures
     * @throws IllegalStateException if this transport was already started or closed
     */
    public synchronized void start(Listener listener) {
        if (this.listener != null || closed.get())
            throw new IllegalStateException("Transport already started/closed");

        this.listener = Objects.requireNonNull(listener);
    }

    /**
     * Registers and submits an HTTP exchange without blocking the caller on connection or response I/O.
     *
     * @param request request whose ID uniquely scopes the new exchange
     * @throws IllegalStateException    if the transport is not open
     * @throws IllegalArgumentException if an exchange with the same ID is already active
     */
    public void send(RpcRequest request) {
        if (listener == null || closed.get())
            throw new IllegalStateException("Transport not open");

        Exchange exchange = new Exchange();

        if (active.putIfAbsent(request.getId(), exchange) != null)
            throw new IllegalArgumentException("Duplicate request");

        try {
            exchange.setTask(io.submit(() -> execute(request, exchange)));

            if (exchange.getCancelled().get())
                exchange.getTask().cancel(true);
        } catch (RuntimeException ex) {
            active.remove(request.getId(), exchange);
            throw ex;
        }
    }

    /**
     * Performs one POST exchange, accepting either a JSON terminal response or an SSE stream ending in one.
     * Local failures are delivered to the listener unless cancellation or shutdown already owns the outcome.
     *
     * @param request  request being sent
     * @param exchange mutable ownership record for cancellation and connection cleanup
     */
    private void execute(RpcRequest request, Exchange exchange) {
        try {
            if (exchange.getCancelled().get())
                return;

            ToolDefinition tool = "tools/call".equals(request.getMethod()) ? tools.get(request.getParams().get("name")) : null;

            // Callers must obtain/register a descriptor first so custom parameter headers cannot silently be omitted.
            if ("tools/call".equals(request.getMethod()) && tool == null)
                throw new IllegalStateException("Call listTools() or transport.tool() before HTTP tools/call");

            Map<String, String> mirrored = HttpMetadata.headers(request, tool);
            Request aj = new Request(HttpMethod.POST, endpoint);
            aj.setContentType("application/json");
            aj.setConnectTimeout(ioTimeoutMs);
            aj.setReadTimeout(ioTimeoutMs);

            HttpURLConnection connection = aj.init(c -> {
                c.setInstanceFollowRedirects(false);
                c.setRequestProperty("Accept", "application/json, text/event-stream");
                headers.forEach(c::setRequestProperty);
                mirrored.forEach(c::setRequestProperty);
            });
            exchange.setConnection(connection);

            if (exchange.getCancelled().get())
                return;

            byte[] bytes = codec.encodeUtf8(request);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(bytes.length);

            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }

            int status = connection.getResponseCode();

            if (status >= 300 && status < 400)
                throw new IOException("Redirect refused");

            InputStream raw = status >= 400 ? connection.getErrorStream() : connection.getInputStream();

            if (raw == null)
                throw new IOException("HTTP " + status + " has no MCP response");

            try (InputStream in = new BufferedInputStream(raw)) {
                String type = connection.getContentType();
                type = type == null ? "" : type.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);

                if ("application/json".equals(type)) {
                    RpcMessage message = codec.decodeUtf8(WireIO.readAll(in));
                    requireResponse(message, request);

                    if (status != 200 && !(message instanceof RpcError))
                        throw new IOException("HTTP error without RPC error");

                    listener.message(request.getId(), message);
                } else if (status == 200 && "text/event-stream".equals(type))
                    readSse(in, request, exchange);
                else
                    throw new IOException("Unsupported HTTP response status/content type: " + status);
            }
        } catch (Throwable ex) {
            if (!exchange.getCancelled().get() && !closed.get())
                listener.failed(request.getId(), ex);
        } finally {
            active.remove(request.getId(), exchange);

            if (exchange.getConnection() != null)
                exchange.getConnection().disconnect();
        }
    }

    /**
     * Verifies that a terminal HTTP message is a response and, when present, carries the originating request ID.
     *
     * @param message decoded HTTP response message
     * @param request originating request
     * @throws IOException if the message is not a final response or has a mismatched ID
     */
    private void requireResponse(RpcMessage message, RpcRequest request) throws IOException {
        if (!(message instanceof RpcSuccess) && !(message instanceof RpcError))
            throw new IOException("Expected final response");

        RequestId id = message instanceof RpcSuccess ? ((RpcSuccess) message).getId() : ((RpcError) message).getId();

        if (id != null && !id.equals(request.getId()))
            throw new IOException("Response ID mismatch");
    }

    /**
     * Reads an SSE response stream, forwarding scoped notifications until the required terminal response arrives.
     * SSE comments and replay hints are intentionally ignored because this transport does not implement replay.
     *
     * @param in       SSE response body
     * @param request  originating request used for response correlation
     * @param exchange cancellation state for the response stream
     * @throws IOException if framing is invalid, an event exceeds the bound, or the stream ends before a response
     */
    private void readSse(InputStream in, RpcRequest request, Exchange exchange) throws IOException {
        StringBuilder data = new StringBuilder();
        String line;
        boolean first = true;

        while (!exchange.getCancelled().get() && (line = WireIO.line(in)) != null) {
            if (first && line.startsWith("\uFEFF"))
                line = line.substring(1);

            first = false;

            if (line.isEmpty()) {
                if (data.length() == 0) continue;
                RpcMessage message = codec.decode(data.substring(0, data.length() - 1));
                data.setLength(0);

                if (message instanceof RpcNotification)
                    listener.message(request.getId(), message);
                else {
                    requireResponse(message, request);
                    listener.message(request.getId(), message);

                    return;
                }
            } else if (line.startsWith("data:") || line.equals("data")) {
                String value = line.equals("data") ? "" : line.substring(5);

                if (value.startsWith(" "))
                    value = value.substring(1);

                if (data.length() + value.length() + 1 > WireIO.MAX_BYTES)
                    throw new IOException("SSE event too large");

                data.append(value).append('\n');
            }
            // Comments, event IDs and retry hints do not create replay/resume behavior.
        }

        if (!exchange.getCancelled().get())
            throw new EOFException("SSE ended without a final response");
    }

    /**
     * Stops the local HTTP task and disconnects its request-scoped connection when one has been established.
     *
     * @param id request identifier whose active exchange should be cancelled
     */
    public void cancel(RequestId id) {
        Exchange exchange = active.remove(id);

        if (exchange != null) {
            exchange.getCancelled().set(true);

            if (exchange.getTask() != null)
                exchange.getTask().cancel(true);

            if (exchange.getConnection() != null) {
                try {
                    cleanup.execute(exchange.getConnection()::disconnect);
                } catch (RejectedExecutionException ignored) {
                    /* The I/O worker also disconnects in finally. */
                }
            }
        }
    }

    /**
     * Idempotent cancels all active HTTP exchanges and shuts down transport-owned executors.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (RequestId id : active.keySet()) cancel(id);
            io.shutdownNow();
            cleanup.shutdown();
        }
    }
}
