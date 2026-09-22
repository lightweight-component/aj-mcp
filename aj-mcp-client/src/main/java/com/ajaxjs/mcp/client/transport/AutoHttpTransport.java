package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.ProtocolVersion;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * HTTP transport that probes Streamable HTTP before falling back to legacy SSE.
 *
 * <p>The initial probe is the only operation eligible for fallback. Authentication,
 * rate-limit, timeout, business, and post-initialization failures are propagated
 * instead of being retried against another protocol. Once selected, the delegate
 * remains stable for the lifetime of this transport.</p>
 *
 * <p>Both delegate transports receive the same pending-request map, callbacks,
 * supported revisions, and headers. Closing this wrapper closes discovery,
 * the selected delegate, and all outstanding requests.</p>
 */
public final class AutoHttpTransport extends McpTransport {
    private final String endpointUrl;
    private final Duration timeout;
    private final Map<String, String> headers;
    private final boolean openEventStream;
    private final ExecutorService discovery = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "aj-mcp-http-discovery");
        thread.setDaemon(true);
        return thread;
    });
    private volatile McpTransport delegate;
    private volatile boolean closed;
    private Map<Long, CompletableFuture<JsonNode>> pending;
    private List<String> versions = ProtocolVersion.supportedVersions();
    private Consumer<JsonNode> notifications;
    private Function<JsonNode, JsonNode> requests;
    private CompletableFuture<JsonNode> initialization;

    /**
     * Creates a transport that discovers the protocol at the supplied endpoint.
     *
     * @param endpointUrl the MCP HTTP endpoint URL
     */
    public AutoHttpTransport(String endpointUrl) {
        this(endpointUrl, true, Duration.ofSeconds(60), null);
    }

    /**
     * Creates a transport with automatic Streamable HTTP and legacy SSE discovery.
     *
     * @param endpointUrl     the MCP HTTP endpoint URL
     * @param openEventStream whether to open the optional Streamable HTTP GET stream
     * @param timeout         the HTTP operation timeout; null or zero uses the default
     * @param requestHeaders  HTTP headers to send, or null for no additional headers
     */
    @Builder
    public AutoHttpTransport(String endpointUrl, boolean openEventStream, Duration timeout,
                             Map<String, String> requestHeaders) {
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "Missing MCP server URL");
        this.openEventStream = openEventStream;
        this.timeout = timeout == null || timeout.isZero() ? Duration.ofSeconds(60) : timeout;
        if (this.timeout.isNegative()) throw new IllegalArgumentException("timeout must not be negative");
        headers = requestHeaders == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));
    }

    /**
     * Starts the Streamable HTTP probe transport.
     *
     * @param pendingRequest session-local map used to correlate responses
     * @throws IllegalStateException if this transport was already started or closed
     */
    @Override
    public synchronized void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
        if (closed || delegate != null) throw new IllegalStateException("Transport already started or closed");
        pending = Objects.requireNonNull(pendingRequest, "pendingRequest");
        setPendingRequests(pending);
        install(new StreamableHttpTransport(endpointUrl, openEventStream, timeout, headers));
        delegate.start(pending);
    }

    private synchronized void install(McpTransport transport) {
        if (closed) {
            try {
                transport.close();
            } catch (IOException ignored) {
            }
            throw new IllegalStateException("HTTP transport is closed");
        }
        transport.setMessageHandlers(notifications, requests);
        transport.setSupportedProtocolVersions(versions);
        delegate = transport;
    }

    /**
     * Initializes the selected protocol, falling back only for an eligible
     * initial HTTP rejection (400, 404, 405, or 415).
     *
     * @param request initialize request containing client capabilities and revisions
     * @return future completed with the server initialize response
     * @throws IllegalStateException if start was not called or initialization was repeated
     */
    @Override
    public synchronized CompletableFuture<JsonNode> initialize(InitializeRequest request) {
        if (closed || delegate == null) throw new IllegalStateException("Transport is not started");
        if (initialization != null) throw new IllegalStateException("Initialization already started");
        initialization = new CompletableFuture<>();
        discovery.execute(() -> {
            try {
                JsonNode result;
                try {
                    result = delegate.initialize(request).get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
                } catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    if (!(cause instanceof HttpStatusException)) throw failure;
                    HttpStatusException http = (HttpStatusException) cause;
                    if (!http.initialization || !(http.status == 400 || http.status == 404
                            || http.status == 405 || http.status == 415)) throw failure;
                    // Discard all resources from the rejected probe before opening legacy SSE.
                    delegate.close();
                    install(new HttpMcpTransport(endpointUrl, false, false, timeout, headers));
                    delegate.start(pending);
                    result = delegate.initialize(request).get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
                }
                initialization.complete(result);
            } catch (Exception failure) {
                if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                initialization.completeExceptionally(failure instanceof ExecutionException ? failure.getCause() : failure);
                close();
            } finally {
                discovery.shutdown();
            }
        });
        initialization.whenComplete((result, error) -> {
            if (initialization.isCancelled()) close();
        });
        return initialization;
    }

    @Override
    public synchronized void setMessageHandlers(Consumer<JsonNode> notificationHandler, Function<JsonNode, JsonNode> requestHandler) {
        notifications = notificationHandler;
        requests = requestHandler;
        if (delegate != null) delegate.setMessageHandlers(notifications, requests);
    }

    /**
     * Sets the protocol versions that may be selected during initialization.
     *
     * @param supported supported protocol version strings
     */
    @Override
    public synchronized void setSupportedProtocolVersions(List<String> supported) {
        versions = new ArrayList<>(supported);
        if (delegate != null) delegate.setSupportedProtocolVersions(versions);
    }

    /**
     * Records the negotiated protocol version and forwards it to the active delegate.
     *
     * @param version the negotiated protocol version
     */
    @Override
    public void setNegotiatedProtocolVersion(String version) {
        super.setNegotiatedProtocolVersion(version);
        if (delegate != null) delegate.setNegotiatedProtocolVersion(version);
    }

    /**
     * Returns the protocol version negotiated by the active delegate.
     *
     * @return the negotiated version, or null before a delegate is selected
     */
    @Override
    public String getNegotiatedProtocolVersion() {
        return delegate == null ? null : delegate.getNegotiatedProtocolVersion();
    }

    @Override
    public void markInitialized() {
        super.markInitialized();
        delegate.markInitialized();
    }

    @Override
    public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
        requireInitialized();
        return delegate.sendRequestWithResponse(request);
    }

    @Override
    public void sendRequestWithoutResponse(McpRequest request) {
        requireInitialized();
        delegate.sendRequestWithoutResponse(request);
    }

    @Override
    protected void sendJson(JsonNode message) {
        delegate.sendJson(message);
    }

    @Override
    public void checkHealth() {
        if (closed || delegate == null) throw new IllegalStateException("HTTP transport is not running");
        delegate.checkHealth();
    }

    /**
     * Reports whether discovery selected the legacy HTTP/SSE transport.
     *
     * @return true after the initial probe selected legacy HTTP/SSE; otherwise false
     */
    public boolean isLegacySse() {
        return delegate instanceof HttpMcpTransport;
    }

    @Override
    public void close() {
        McpTransport current;
        synchronized (this) {
            if (closed) return;
            closed = true;
            current = delegate;
            if (initialization != null)
                initialization.completeExceptionally(new IOException("HTTP transport closed"));
        }
        discovery.shutdownNow();
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
            }
        }
        failPendingRequests(new IOException("HTTP transport closed"));
    }
}
