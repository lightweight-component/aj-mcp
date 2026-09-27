package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.core.*;
import com.ajaxjs.mcp2026.server.model.Connection;
import com.ajaxjs.mcp2026.server.model.Authentication;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight JDK HTTP adapter. Inject authentication for non-loopback deployments (normally behind TLS).
 */
public final class HttpServerTransport implements AutoCloseable {
    /**
     * Instance-scoped dispatcher receiving validated HTTP requests.
     */
    private final McpServer server;
    /**
     * JDK listener owned and stopped by this transport.
     */
    private final HttpServer http;
    /**
     * Exact endpoint path accepted by the registered HTTP context.
     */
    private final String path;
    /**
     * Immutable allowlist for browser Origin headers; it is not an authentication mechanism.
     */
    private final Set<String> origins;
    /**
     * Authentication hook invoked before protocol parsing and connection creation.
     */
    private final Authentication authentication;
    /**
     * Bounded executor that owns JDK HTTP request handling.
     */
    private final ExecutorService executor;
    /**
     * Strict codec used for request parsing and safe response serialization.
     */
    private final McpCodec codec = new McpCodec();
    /**
     * Idempotent lifecycle flag preventing restart or duplicate shutdown.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Creates a loopback-only server at {@code /mcp} with no origin allowlist and anonymous authentication.
     *
     * @param server server dispatcher
     * @param port   loopback port, or zero for an ephemeral port
     * @throws IOException if the JDK HTTP server cannot bind the port
     */
    public HttpServerTransport(McpServer server, int port) throws IOException {
        this(server, new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), "/mcp", Collections.emptySet(), e -> null, 32);
    }

    /**
     * Creates a request-scoped HTTP/SSE MCP transport. Production non-loopback deployments should supply TLS,
     * authentication, and an explicit Origin allowlist at the surrounding HTTP boundary.
     *
     * @param server         server dispatcher
     * @param address        listening address
     * @param path           absolute endpoint path without query or fragment
     * @param origins        allowed Origin values; an empty set rejects requests carrying Origin
     * @param authentication authentication hook invoked before request dispatch
     * @param concurrency    maximum concurrent HTTP handlers
     * @throws IOException if the JDK HTTP server cannot bind the address
     */
    public HttpServerTransport(McpServer server, InetSocketAddress address, String path, Set<String> origins,
                               Authentication authentication, int concurrency) throws IOException {
        if (!path.startsWith("/") || path.contains("?") || path.contains("#") || concurrency < 1)
            throw new IllegalArgumentException("Invalid HTTP configuration");
        this.server = Objects.requireNonNull(server);
        this.path = path;
        this.origins = Collections.unmodifiableSet(new HashSet<>(origins));
        this.authentication = Objects.requireNonNull(authentication);

        for (ToolDefinition tool : server.toolDefinitions())
            HttpMetadata.validateTool(tool);

        http = HttpServer.create(address, 0);
        executor = new ThreadPoolExecutor(concurrency, concurrency, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(128),
                McpServer.daemon("mcp-http-server"), new ThreadPoolExecutor.AbortPolicy());
        http.setExecutor(executor);
        http.createContext(path, this::handle);
    }

    /**
     * Starts accepting requests; the transport cannot be restarted after close.
     */
    public void start() {
        if (closed.get())
            throw new IllegalStateException("Closed");

        http.start();
    }

    /**
     * Returns the bound endpoint after construction, including an OS-selected ephemeral port.
     *
     * @return HTTP MCP endpoint URI
     */
    public URI endpoint() {
        return URI.create("http://" + http.getAddress().getHostString() + ":" + http.getAddress().getPort() + path);
    }

    /**
     * Authenticates, validates, and serves one HTTP POST as a request-scoped SSE response stream.
     *
     * @param exchange JDK server exchange to consume and close
     * @throws IOException if an HTTP response or SSE stream cannot be written
     */
    private void handle(HttpExchange exchange) throws IOException {
        Connection connection = null;

        try {
            Headers headers = exchange.getRequestHeaders();
            List<String> origin = headers.get("Origin");

            if (origin != null && (origin.size() != 1 || !origins.contains(origin.get(0)))) {
                empty(exchange, 403);

                return;
            }

            if (!path.equals(exchange.getRequestURI().getRawPath()) || exchange.getRequestURI().getRawQuery() != null) {
                empty(exchange, 404);

                return;
            }

            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                empty(exchange, 405);

                return;
            }

            Principal principal;

            try {
                principal = authentication.authenticate(exchange);
            } catch (SecurityException denied) {
                empty(exchange, 401);

                return;
            }

            String content = headers.getFirst("Content-Type");

            if (content == null || !"application/json".equalsIgnoreCase(content.split(";", 2)[0].trim())) {
                empty(exchange, 415);

                return;
            }

            String accept = headers.getFirst("Accept");

            if (accept == null || !accept.toLowerCase(Locale.ROOT).contains("application/json") || !accept.toLowerCase(Locale.ROOT).contains("text/event-stream")) {
                empty(exchange, 406);

                return;
            }

            RpcMessage message;

            try {
                message = codec.decodeUtf8(WireIO.readAll(exchange.getRequestBody()));
            } catch (McpProtocolException ex) {
                if (ex.isNotification())
                    empty(exchange, 400);
                else
                    json(exchange, 400, ex.toErrorResponse());

                return;
            } catch (IOException ex) {
                empty(exchange, 413);

                return;
            }

            if (message instanceof RpcNotification) {
                empty(exchange, 202);

                return;
            }

            if (!(message instanceof RpcRequest)) {
                empty(exchange, 400);

                return;
            }

            RpcRequest request = (RpcRequest) message;

            try {
                server.getProtocol().requireRequest(request);
                ToolDefinition tool = "tools/call".equals(request.getMethod()) ? server.toolDefinition((String) request.getParams().get("name")) : null;
                HttpMetadata.require(request, tool, headers);
            } catch (McpProtocolException ex) {
                json(exchange, ex.getError().getCode() == ErrorCodes.METHOD_NOT_FOUND ? 404 : 400, ex.getError());

                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
            exchange.sendResponseHeaders(200, 0);
            OutputStream output = exchange.getResponseBody();

            connection = server.connect(response -> {
                byte[] data = ("data: " + codec.encode(response) + "\n\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);

                synchronized (output) {
                    output.write(data);
                    output.flush();
                }
            }, principal, true);

            CompletableFuture<Void> done = connection.receive(request);

            while (!done.isDone()) {
                try {
                    done.get(1, TimeUnit.SECONDS);
                } catch (TimeoutException heartbeat) {
                    synchronized (output) {
                        output.write(": keepalive\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        output.flush();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();

                    return;
                } catch (ExecutionException failed) {
                    return;
                }
            }
        } finally {
            if (connection != null)
                connection.close();

            exchange.close();
        }
    }

    /**
     * Writes a non-streaming JSON-RPC response, or an empty HTTP response when no message is available.
     *
     * @param exchange HTTP exchange receiving the response
     * @param status   HTTP status code to emit
     * @param message  protocol message, or {@code null} for an empty response
     * @throws IOException if headers or the response body cannot be written
     */
    private void json(HttpExchange exchange, int status, RpcMessage message) throws IOException {
        if (message == null) {
            empty(exchange, status);

            return;
        }

        byte[] body = codec.encodeUtf8(message);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    /**
     * Completes an exchange with a status and no response body.
     *
     * @param exchange HTTP exchange to complete
     * @param status   HTTP status code to emit
     * @throws IOException if response headers cannot be written
     */
    private static void empty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            http.stop(0);
            executor.shutdownNow();
        }
    }
}
