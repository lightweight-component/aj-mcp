package com.ajaxjs.mcp2026.client;

import com.ajaxjs.mcp2026.client.model.Pending;
import com.ajaxjs.mcp2026.client.model.Subscription;
import com.ajaxjs.mcp2026.core.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Thread-safe modern-only client. No initialized session, implicit replay, or shared authorization cache.
 */
public final class McpClient implements AutoCloseable {
    /**
     * Default finite deadline used when an API timeout argument is zero, in milliseconds.
     */
    public static final long DEFAULT_TIMEOUT_MS = 30000;

    /**
     * Exclusive asynchronous transport that owns wire I/O and peer cancellation.
     */
    private final ClientTransport transport;

    /**
     * Immutable per-request version and client capabilities inserted into every generated request.
     */
    private final RequestMetadata metadata;

    /**
     * Shared official-schema validator for outbound requests and inbound messages.
     */
    private final ProtocolSchema protocol = new ProtocolSchema();

    /**
     * In-flight requests keyed by generated JSON-RPC ID for correlation, deadlines, and cancellation.
     */
    private final ConcurrentMap<RequestId, Pending> pending = new ConcurrentHashMap<>();

    /**
     * Monotonic suffix source for client-local request identifiers.
     */
    private final AtomicLong sequence = new AtomicLong();

    /**
     * Random per-client request-ID namespace preventing collisions across client instances.
     */
    private final String prefix = UUID.randomUUID() + "-";

    /**
     * Single scheduler enforcing finite ordinary-request and MRTR deadlines.
     */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(daemon("mcp-client-timeout"));

    /**
     * Bounded worker pool that drives opted-in multi-round-trip requests.
     */
    private final ExecutorService rounds = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(128), daemon("mcp-client-input"), new ThreadPoolExecutor.AbortPolicy());
    /**
     * Idempotent close state checked before registering or sending work.
     */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * MRTR result futures completed exceptionally during the client shutdown.
     */
    private final Set<CompletableFuture<McpResult>> executions = ConcurrentHashMap.newKeySet();

    /**
     * Application callback for validated request-scoped notifications.
     */
    private final Consumer<RpcNotification> notifications;

    /**
     * Creates a client with empty client capabilities and no-op handling for non-subscription notifications.
     *
     * @param transport exclusive asynchronous transport owned by this client
     */
    public McpClient(ClientTransport transport) {
        this(transport, RequestMetadata.create(Collections.emptyMap()), n -> {
        });
    }

    /**
     * Creates and starts a stateless MCP 2026 client.
     * Metadata is included in each generated request; it is not negotiated or cached by connection.
     *
     * @param transport     exclusive asynchronous transport owned by this client
     * @param metadata      required per-request version and client capabilities
     * @param notifications consumer for validated request-scoped notifications
     */
    public McpClient(ClientTransport transport, RequestMetadata metadata, Consumer<RpcNotification> notifications) {
        this.transport = Objects.requireNonNull(transport);
        this.metadata = Objects.requireNonNull(metadata);
        this.notifications = Objects.requireNonNull(notifications);

        try {
            transport.start(new ClientTransport.Listener() {
                public void message(RequestId scope, RpcMessage message) {
                    receive(scope, message);
                }

                public void failed(RequestId scope, Throwable error) {
                    if (scope == null)
                        failAll(error);
                    else
                        fail(scope, error);
                }
            });
        } catch (RuntimeException ex) {
            close();
            throw ex;
        }
    }

    /**
     * Creates daemon threads with a stable descriptive name for client-owned executors.
     *
     * @param name thread name
     * @return daemon thread factory
     */
    public static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);

            return t;
        };
    }

    /**
     * Allocates a client-local request ID whose random prefix prevents cross-instance collisions.
     *
     * @return next unique request identifier for this client instance
     */
    private RequestId nextId() {
        return RequestId.of(prefix + sequence.incrementAndGet());
    }

    /**
     * Builds a request with a unique client-local ID and this client's per-request metadata.
     *
     * @param method MCP method name
     * @param params method-specific object parameters, excluding {@code _meta}
     * @return request ready for protocol validation and transmission
     */
    public RpcRequest request(String method, Map<String, Object> params) {
        return RpcRequest.create(nextId(), method, params, metadata);
    }

    /**
     * Sends one request asynchronously and completes with its successful MCP result.
     * A remote JSON-RPC error completes the future exceptionally as {@link McpRemoteException}; cancelling the
     * returned future propagates cancellation to the underlying request.
     *
     * @param method    MCP method name
     * @param params    method-specific object parameters
     * @param timeoutMs positive deadline in milliseconds, or zero for {@link #DEFAULT_TIMEOUT_MS}
     * @return result future
     */
    public CompletableFuture<McpResult> callAsync(String method, Map<String, Object> params, long timeoutMs) {
        CompletableFuture<RpcSuccess> source = exchange(request(method, params), timeoutMs, null);
        CompletableFuture<McpResult> result = source.thenApply(RpcSuccess::getResult);
        result.whenComplete((v, e) -> {
            if (result.isCancelled())
                source.cancel(true);
        });

        return result;
    }

    /**
     * Sends a caller-created request and completes with its full success envelope.
     * Cancellation of this future propagates to the actual request. The request is validated against the official
     * MCP schema before it is registered with the transport.
     *
     * @param request   complete request containing the required per-request metadata
     * @param timeoutMs positive deadline in milliseconds, or zero for {@link #DEFAULT_TIMEOUT_MS}
     * @return future for the correlated success response
     */
    public CompletableFuture<RpcSuccess> exchange(RpcRequest request, long timeoutMs) {
        return exchange(request, timeoutMs, null);
    }

    /**
     * Registers one validated request, installs deadline and cancellation cleanup, then hands it to the transport.
     *
     * @param request      complete outbound request
     * @param timeoutMs    finite deadline, or zero for the default
     * @param subscription lifecycle state for a listen request, or {@code null}
     * @return correlated success-response future
     */
    private CompletableFuture<RpcSuccess> exchange(RpcRequest request, long timeoutMs, Subscription subscription) {
        if (timeoutMs < 0)
            throw new IllegalArgumentException("Negative timeout");

        protocol.requireRequest(request);

        if (closed.get())
            throw new IllegalStateException("Client closed");

        Pending p = new Pending(request, subscription);

        if (pending.putIfAbsent(request.getId(), p) != null)
            throw new IllegalArgumentException("Duplicate in-flight ID");

        CompletableFuture<RpcSuccess> future = p.getFuture();

        future.whenComplete((result, error) -> {
            pending.remove(request.getId(), p);
            ScheduledFuture<?> deadline = p.getDeadline();

            if (deadline != null)
                deadline.cancel(false);

            synchronized (p) {
                if (error != null && p.isSent())
                    transport.cancel(request.getId());
            }

            if (subscription != null && !subscription.getAcknowledged().isDone())
                subscription.getAcknowledged().completeExceptionally(error != null ? error : new IllegalStateException("Subscription ended before acknowledgment"));
        });

        try {
            p.setDeadline(timer.schedule(() -> future.completeExceptionally(new TimeoutException("MCP request deadline exceeded")),
                    timeoutMs == 0 ? DEFAULT_TIMEOUT_MS : timeoutMs, TimeUnit.MILLISECONDS));

            if (future.isDone())
                p.getDeadline().cancel(false);

            if (closed.get())
                future.completeExceptionally(new IllegalStateException("Client closed"));

            synchronized (p) {
                if (!future.isDone()) {
                    p.setSent(true);
                    transport.send(request);
                }
            }
        } catch (RuntimeException ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    /**
     * Sends one request and waits for its result, translating interruption and asynchronous failure to
     * {@link CompletionException}.
     *
     * @param method    MCP method name
     * @param params    method-specific object parameters
     * @param timeoutMs positive deadline in milliseconds, or zero for the default deadline
     * @return successful MCP result
     */
    public McpResult call(String method, Map<String, Object> params, long timeoutMs) {
        CompletableFuture<RpcSuccess> future = exchange(request(method, params), timeoutMs);

        return await(future).getResult();
    }

    /**
     * Calls {@code server/discover} to obtain the server implementation and advertised capabilities.
     *
     * @return validated discovery result
     */
    public McpResult discover() {
        return call(Discovery.METHOD, Collections.emptyMap(), 0);
    }

    /**
     * Retrieves every page of {@code tools/list} and registers HTTP-safe descriptors with the transport.
     * Invalid HTTP parameter-header descriptors are omitted rather than silently used for a tool call.
     *
     * @return immutable list of accepted tool descriptors
     */
    public List<ToolDefinition> listTools() {
        List<ToolDefinition> tools = new ArrayList<>();

        for (Object item : listAll("tools/list", "tools", 100)) {
            @SuppressWarnings("unchecked") ToolDefinition tool = ToolDefinition.from((Map<String, Object>) item);

            if (transport.isHttp()) {
                try {
                    HttpMetadata.validateTool(tool);
                } catch (IllegalArgumentException invalid) {
                    continue;
                }
            }

            transport.tool(tool);
            tools.add(tool);
        }

        return Collections.unmodifiableList(tools);
    }

    /**
     * Calls {@code tools/call}; HTTP transports first refresh the tool list so required parameter headers can be set.
     *
     * @param name      name of a server-advertised tool
     * @param arguments tool argument object
     * @return complete or input-required tool result
     */
    public McpResult callTool(String name, Map<String, Object> arguments) {
        if (transport.isHttp()) {
            boolean found = false;

            for (ToolDefinition tool : listTools())
                if (tool.getName().equals(name)) {
                    found = true;
                    break;
                }

            if (!found)
                throw new IllegalArgumentException("Tool is not in the current HTTP tool list");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", name);
        params.put("arguments", arguments);

        return call("tools/call", params, 0);
    }

    /**
     * Follows opaque {@code nextCursor} values for a paginated list result until the final page.
     * Repeated cursors and a missing list field are rejected to prevent loops or schema confusion.
     *
     * @param method   list method to invoke
     * @param field    result field containing each page's list
     * @param maxPages positive upper bound on page requests
     * @return accumulated items in server order
     */
    public List<Object> listAll(String method, String field, int maxPages) {
        if (maxPages < 1)
            throw new IllegalArgumentException("maxPages must be positive");

        List<Object> result = new ArrayList<>();
        Set<String> cursors = new HashSet<>();
        Map<String, Object> params = Collections.emptyMap();

        for (int page = 0; page < maxPages; page++) {
            Map<String, Object> fields = call(method, params, 0).toMap();
            Object values = fields.get(field);

            if (!(values instanceof List))
                throw new IllegalArgumentException("Result is not a list");

            result.addAll((List<?>) values);
            Object cursor = fields.get("nextCursor");

            if (cursor == null)
                return result;

            if (!(cursor instanceof String) || !cursors.add((String) cursor))
                throw new IllegalStateException("Repeated/invalid cursor");

            params = Collections.singletonMap("cursor", cursor);
        }

        throw new IllegalStateException("Pagination limit exceeded");
    }

    /**
     * Starts a long-lived {@code subscriptions/listen} request and returns its acknowledgement/completion lifecycle.
     * The callback receives only notifications accepted by the acknowledgement filter and protocol validator.
     *
     * @param filter     requested notification filter
     * @param lifetimeMs positive request lifetime in milliseconds, or zero for the default deadline
     * @param updates    consumer for validated stream notifications
     * @return subscription lifecycle handle
     */
    public Subscription subscribe(Map<String, Object> filter, long lifetimeMs, Consumer<RpcNotification> updates) {
        RpcRequest request = Subscriptions.listen(nextId(), metadata, filter);
        Subscription sub = new Subscription(request, Objects.requireNonNull(updates), protocol);
        sub.setCompletion(exchange(request, lifetimeMs, sub));

        return sub;
    }

    /**
     * Input execution is opted in: the SDK never approves elicitation or runs a model by itself.
     */
    public interface InputHandler {
        /**
         * Produces response values for an {@code input_required} MRTR result.
         * Implementations decide whether and how to obtain user, model, or application input.
         *
         * @param inputRequired validated result describing the requested input
         * @return JSON object used as {@code inputResponses} in the retry
         * @throws Exception if input cannot be obtained or approved
         */
        Map<String, Object> respond(McpResult inputRequired) throws Exception;
    }

    /**
     * Drives an opted-in multi-round-trip request until it completes, reaches its deadline, or reaches the round limit.
     * The SDK never automatically approves elicitation or invokes a model: every {@code input_required} result is
     * delegated to {@code inputs}, then validated before a retry is sent.
     *
     * @param method    initial MCP method name
     * @param params    initial method parameters
     * @param timeoutMs whole-operation deadline in milliseconds, or zero for the default deadline
     * @param maxRounds positive maximum number of request/response rounds
     * @param inputs    application-controlled input provider
     * @return future for the final non-{@code input_required} result
     */
    public CompletableFuture<McpResult> callWithInputs(String method, Map<String, Object> params,
                                                       long timeoutMs, int maxRounds, InputHandler inputs) {
        if (timeoutMs < 0 || maxRounds < 1)
            throw new IllegalArgumentException("Invalid execution bounds");

        Objects.requireNonNull(inputs);

        if (closed.get())
            throw new IllegalStateException("Client closed");

        CompletableFuture<McpResult> result = new CompletableFuture<>();
        executions.add(result);
        AtomicReference<CompletableFuture<RpcSuccess>> active = new AtomicReference<>();
        AtomicReference<Future<?>> worker = new AtomicReference<>();
        long millis = timeoutMs == 0 ? DEFAULT_TIMEOUT_MS : timeoutMs;
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.min(millis, TimeUnit.DAYS.toMillis(365)));
        AtomicReference<ScheduledFuture<?>> alarm = new AtomicReference<>();

        result.whenComplete((v, e) -> {
            executions.remove(result);

            if (alarm.get() != null)
                alarm.get().cancel(false);

            CompletableFuture<?> f = active.get();

            if (f != null && !f.isDone())
                f.cancel(true);

            Future<?> task = worker.get();

            if (task != null && !task.isDone())
                task.cancel(true);
        });
        try {
            alarm.set(timer.schedule(() -> result.completeExceptionally(new TimeoutException("MRTR deadline exceeded")), millis, TimeUnit.MILLISECONDS));

            if (result.isDone())
                alarm.get().cancel(false);

            if (closed.get()) {
                result.completeExceptionally(new IllegalStateException("Client closed"));
                return result;
            }

            worker.set(rounds.submit(() -> {
                try {
                    RpcRequest req = request(method, params);

                    for (int round = 0; round < maxRounds && !result.isDone(); round++) {
                        long left = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());

                        if (left <= 0)
                            throw new TimeoutException("MRTR deadline exceeded");

                        CompletableFuture<RpcSuccess> f = exchange(req, left);
                        active.set(f);

                        if (result.isDone()) {
                            f.cancel(true);
                            return;
                        }

                        RpcSuccess response = await(f);

                        if (!McpResult.INPUT_REQUIRED.equals(response.getResult().getResultType())) {
                            result.complete(response.getResult());
                            return;
                        }

                        if (round + 1 == maxRounds)
                            throw new IllegalStateException("MRTR round limit exceeded");

                        Map<String, Object> answers = inputs.respond(response.getResult());
                        RpcRequest retry = MultiRoundTrip.retry(req, response, nextId(), answers);
                        protocol.requireRetry(req, response, retry);
                        req = retry;
                    }
                } catch (Throwable ex) {
                    if (ex instanceof InterruptedException)
                        Thread.currentThread().interrupt();

                    result.completeExceptionally(ex);
                }
            }));

            if (result.isDone())
                worker.get().cancel(true);
        } catch (RuntimeException ex) {
            result.completeExceptionally(ex);
        }

        return result;
    }

    /**
     * Correlates one transport-delivered message, validates its protocol role, and completes its pending request.
     * Scoped HTTP notifications cannot cross requests; unscoped STDIO notifications are accepted only when valid for
     * an active request.
     *
     * @param scope   originating HTTP request ID, or {@code null} for STDIO
     * @param message decoded peer message
     */
    private void receive(RequestId scope, RpcMessage message) {
        try {
            if (message instanceof RpcNotification) {
                RpcNotification n = (RpcNotification) message;
                protocol.requireNotification(n);

                if (Subscriptions.ACKNOWLEDGED.equals(n.getMethod()) || (n.getParams() != null && n.getParams().get("_meta") instanceof Map
                        && ((Map<?, ?>) n.getParams().get("_meta")).containsKey(Subscriptions.SUBSCRIPTION_ID))) {
                    RequestId id = Subscriptions.notificationId(n);
                    Pending p = pending.get(id);

                    if (scope != null && !scope.equals(id))
                        throw new IllegalArgumentException("Cross-request subscription message");

                    if (p == null)
                        return;

                    if (p.getSubscription() == null)
                        throw new IllegalArgumentException("Unsolicited subscription");

                    p.getSubscription().receive(n);

                    return;
                }
                if ("notifications/cancelled".equals(n.getMethod())) {
                    if (transport.isHttp())
                        throw new IllegalArgumentException("HTTP cannot send cancellation notifications");

                    RequestId id = RequestId.from(n.getParams().get("requestId"));
                    Pending p = pending.get(id);

                    if (p != null) {
                        protocol.requireRequestNotification(p.getRequest(), n);
                        p.getFuture().cancel(false);
                    }

                    return;
                }

                boolean allowed = false;

                if (scope != null) {
                    Pending p = pending.get(scope);

                    if (p == null)
                        return;

                    protocol.requireRequestNotification(p.getRequest(), n);
                    allowed = true;
                } else
                    for (Pending p : pending.values()) {
                        try {
                            protocol.requireRequestNotification(p.getRequest(), n);
                            allowed = true;
                            break;
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                if (!allowed)
                    return; // A queued STDIO notification may race a local deadline/cancellation.

                notifications.accept(n);

                return;
            }

            RequestId id = message instanceof RpcSuccess ? ((RpcSuccess) message).getId()
                    : message instanceof RpcError ? ((RpcError) message).getId() : null;

            if (scope != null && id != null && !scope.equals(id))
                throw new IllegalArgumentException("Response ID mismatch");

            if (id == null) {
                if (scope != null && message instanceof RpcError)
                    fail(scope, new McpRemoteException((RpcError) message));
                else
                    throw new IllegalArgumentException("Uncorrelated server message");

                return;
            }

            Pending p = pending.get(id);

            if (p == null)
                return;

            if (message instanceof RpcError)
                p.getFuture().completeExceptionally(new McpRemoteException((RpcError) message));

            else {
                RpcSuccess response = (RpcSuccess) message;
                protocol.requireResult(p.getRequest(), response);

                if (transport.isHttp() && "tools/list".equals(p.getRequest().getMethod())) {
                    Map<String, Object> fields = new LinkedHashMap<>(response.getResult().toMap());
                    List<Object> accepted = new ArrayList<>();

                    for (Object item : (List<?>) fields.get("tools")) {
                        @SuppressWarnings("unchecked") ToolDefinition tool = ToolDefinition.from((Map<String, Object>) item);

                        try {
                            HttpMetadata.validateTool(tool);
                        } catch (IllegalArgumentException invalid) {
                            continue;
                        }

                        transport.tool(tool);
                        accepted.add(tool.toMap());
                    }

                    fields.put("tools", accepted);
                    response = new RpcSuccess(response.getId(), McpResult.from(fields));
                }

                if (p.getSubscription() != null && p.getSubscription().getAck() == null)
                    throw new IllegalArgumentException("Completion before acknowledgment");

                p.getFuture().complete(response);
            }
        } catch (RuntimeException ex) {
            if (scope == null)
                failAll(ex);
            else
                fail(scope, ex);
        }
    }

    /**
     * Completes one pending request exceptionally when its transport exchange fails locally.
     *
     * @param id affected request identifier
     * @param ex local failure
     */
    private void fail(RequestId id, Throwable ex) {
        Pending p = pending.get(id);

        if (p != null)
            p.getFuture().completeExceptionally(ex);
    }

    /**
     * Fails every pending request after a transport-wide failure and closes client-owned resources.
     *
     * @param ex transport-wide local failure
     */
    private void failAll(Throwable ex) {
        for (Pending p : pending.values())
            p.getFuture().completeExceptionally(ex);

        close();
    }

    /**
     * Returns the number of currently registered outbound requests.
     *
     * @return pending request count
     */
    public int pendingCount() {
        return pending.size();
    }

    /**
     * Waits for a future, cancelling it and restoring the interrupt flag if the caller is interrupted.
     *
     * @param future future to await
     * @param <T>    successful value type
     * @return future's successful value
     * @throws CompletionException when interrupted or when the future completed exceptionally
     */
    public static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            future.cancel(true);
            Thread.currentThread().interrupt();

            throw new CompletionException(ex);
        } catch (ExecutionException ex) {
            throw new CompletionException(ex.getCause());
        }
    }

    /**
     * Idempotently fails pending work, stops client-owned deadline/MRTR executors, and closes the transport.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (CompletableFuture<McpResult> execution : executions)
                execution.completeExceptionally(new IllegalStateException("Client closed"));

            for (Pending p : pending.values())
                p.getFuture().completeExceptionally(new IllegalStateException("Client closed"));

            timer.shutdownNow();
            rounds.shutdownNow();
            transport.close();
        }
    }
}
