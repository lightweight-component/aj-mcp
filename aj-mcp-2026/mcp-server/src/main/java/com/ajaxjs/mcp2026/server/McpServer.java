package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.core.*;
import com.ajaxjs.mcp2026.core.schema.DefaultSchemaValidator;
import com.ajaxjs.mcp2026.server.model.*;
import lombok.Getter;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Instance-scoped registration and bounded dispatch, independent of any HTTP framework.
 */
public final class McpServer implements AutoCloseable, ConnectionRuntime {
    /**
     * Server implementation name reported by {@code server/discover}.
     */
    private final String name;
    /**
     * Server implementation version reported by {@code server/discover}.
     */
    private final String version;
    /**
     * Shared official-schema validator for inbound messages and outbound results.
     */
    @Getter
    private final ProtocolSchema protocol = new ProtocolSchema();
    /**
     * Instance-scoped tool registry keyed by protocol tool name.
     */
    private final ConcurrentMap<String, ToolEntry> tools = new ConcurrentSkipListMap<>();
    /**
     * Instance-scoped resource registry keyed by URI.
     */
    private final ConcurrentMap<String, Entry> resources = new ConcurrentSkipListMap<>();
    /**
     * Instance-scoped prompt registry keyed by prompt name.
     */
    private final ConcurrentMap<String, Entry> prompts = new ConcurrentSkipListMap<>();
    /**
     * Explicit handlers overriding supported built-in MCP methods.
     */
    private final ConcurrentMap<String, Handler> methods = new ConcurrentHashMap<>();
    /**
     * Open transport connections, closed when the server is closed.
     */
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();
    /**
     * Active subscription contexts that receive registry-change notifications.
     */
    private final Set<Context> subscriptions = ConcurrentHashMap.newKeySet();
    /**
     * Bounded worker pool for ordinary request handlers.
     */
    private final ExecutorService workers;
    /**
     * Scheduler for request and subscription lifetime limits.
     */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(daemon("mcp-server-timeout"));
    /**
     * Maximum lifetime of one non-subscription request, in milliseconds.
     */
    @Getter
    private final long requestTimeoutMs;

    /**
     * Maximum lifetime of one subscription listen request, in milliseconds.
     */
    @Getter
    private final long subscriptionLifetimeMs;
    /**
     * Idempotent server shutdown state.
     */
    private final AtomicBoolean closed = new AtomicBoolean();
    /**
     * Application error reporter isolated from protocol dispatch.
     */
    private final Consumer<Throwable> errors;

    /**
     * Creates a server with default bounded worker and lifetime settings.
     *
     * @param name    server implementation name
     * @param version server implementation version
     */
    public McpServer(String name, String version) {
        this(name, version, 8, 30000, 300000, e -> {
        });
    }

    /**
     * Creates an instance-scoped server with explicit concurrency and finite lifecycle bounds.
     *
     * @param name                   server implementation name
     * @param version                server implementation version
     * @param concurrency            positive handler-worker count
     * @param requestTimeoutMs       positive ordinary request deadline
     * @param subscriptionLifetimeMs positive listen-request lifetime
     * @param errors                 local failure reporter isolated from dispatch
     */
    public McpServer(String name, String version, int concurrency, long requestTimeoutMs, long subscriptionLifetimeMs, Consumer<Throwable> errors) {
        this.name = Objects.requireNonNull(name);
        this.version = Objects.requireNonNull(version);
        this.errors = Objects.requireNonNull(errors);

        if (concurrency < 1 || requestTimeoutMs <= 0 || subscriptionLifetimeMs <= 0)
            throw new IllegalArgumentException("Finite positive bounds required");

        this.requestTimeoutMs = requestTimeoutMs;
        this.subscriptionLifetimeMs = subscriptionLifetimeMs;

        workers = new ThreadPoolExecutor(concurrency, concurrency, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(128),
                daemon("mcp-server-worker"), new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Creates named daemon threads for server-owned executors.
     *
     * @param name thread name
     * @return daemon thread factory
     */
    static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);

            return t;
        };
    }

    /**
     * Registers or replaces a tool and emits the tools-list-changed notification to matching subscriptions.
     *
     * @param definition complete tool descriptor
     * @param handler    application handler for {@code tools/call}
     * @return this server
     */
    public McpServer tool(ToolDefinition definition, Handler handler) {
        ToolEntry entry = new ToolEntry(definition, Objects.requireNonNull(handler), new ToolContract(definition, new DefaultSchemaValidator(), protocol));
        tools.put(definition.getName(), entry);
        changed("notifications/tools/list_changed", Collections.emptyMap());

        return this;
    }

    /**
     * Registers or replaces a resource descriptor after official-schema validation.
     *
     * @param descriptor resource descriptor
     * @param handler    application handler for {@code resources/read}
     * @return this server
     */
    public McpServer resource(Map<String, Object> descriptor, Handler handler) {
        Map<String, Object> snapshot = snapshot(descriptor);
        protocol.requireType("Resource", snapshot);
        resources.put((String) snapshot.get("uri"), new Entry(snapshot, Objects.requireNonNull(handler)));
        changed("notifications/resources/list_changed", Collections.emptyMap());

        return this;
    }

    /**
     * Registers or replaces a prompt descriptor after official-schema validation.
     *
     * @param descriptor prompt descriptor
     * @param handler    application handler for {@code prompts/get}
     * @return this server
     */
    public McpServer prompt(Map<String, Object> descriptor, Handler handler) {
        Map<String, Object> snapshot = snapshot(descriptor);
        protocol.requireType("Prompt", snapshot);
        prompts.put((String) snapshot.get("name"), new Entry(snapshot, Objects.requireNonNull(handler)));
        changed("notifications/prompts/list_changed", Collections.emptyMap());

        return this;
    }

    /**
     * Overrides a known core method, e.g. resource templates, authorization-aware listing or completion.
     *
     * @param method  supported core method name
     * @param handler replacement handler
     * @return this server
     */
    public McpServer method(String method, Handler handler) {
        if (!Arrays.asList("tools/list", "tools/call", "resources/list", "resources/templates/list", "resources/read",
                "prompts/list", "prompts/get", "completion/complete").contains(method))
            throw new IllegalArgumentException("Unsupported handler method");

        methods.put(method, Objects.requireNonNull(handler));

        return this;
    }

    /**
     * Removes a registered tool and notifies matching subscriptions when it existed.
     *
     * @param name tool name
     * @return whether a tool was removed
     */
    public boolean removeTool(String name) {
        boolean removed = tools.remove(name) != null;

        if (removed)
            changed("notifications/tools/list_changed", Collections.emptyMap());

        return removed;
    }

    /**
     * Looks up a registered tool descriptor.
     *
     * @param name tool name
     * @return descriptor, or {@code null} when absent
     */
    public ToolDefinition toolDefinition(String name) {
        ToolEntry entry = tools.get(name);

        return entry == null ? null : entry.getDefinition();
    }

    /**
     * Returns the current registered descriptors in deterministic name order.
     *
     * @return registered tool descriptors
     */
    public Collection<ToolDefinition> toolDefinitions() {
        List<ToolDefinition> result = new ArrayList<>();

        for (ToolEntry entry : tools.values())
            result.add(entry.getDefinition());

        return result;
    }

    /**
     * Emits a resource-updated notification to subscriptions that accepted the corresponding capability.
     *
     * @param uri updated resource URI
     */
    public void resourceUpdated(String uri) {
        changed("notifications/resources/updated", Collections.singletonMap("uri", uri));
    }

    /**
     * Delivers one registry-change notification to active subscriptions without allowing one rejected notification
     * to stop the others.
     *
     * @param method notification method
     * @param fields method-specific notification fields
     */
    private void changed(String method, Map<String, Object> fields) {
        for (Context context : subscriptions) {
            Map<String, Object> params = new LinkedHashMap<>(fields);
            params.put("_meta", Collections.singletonMap(Subscriptions.SUBSCRIPTION_ID, context.getRequest().getId().getValue()));

            try {
                context.notify(new RpcNotification(method, params));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /**
     * Creates a transport-scoped connection with isolated request IDs, cancellation, and output serialization.
     *
     * @param sink      transport message sink
     * @param principal authenticated principal, or {@code null}
     * @param http      whether HTTP stream rules apply
     * @return open connection
     */
    public Connection connect(Sink sink, Principal principal, boolean http) {
        if (closed.get())
            throw new IllegalStateException("Server closed");

        Connection connection = new Connection(Objects.requireNonNull(sink), principal, http, this);
        connections.add(connection);

        if (closed.get())
            connection.close();

        return connection;
    }

    /**
     * Dispatches a validated request to an explicit override or built-in feature registry.
     *
     * @param context request execution context
     * @return method result
     * @throws Exception if an application handler cannot complete normally
     */
    @Override
    public McpResult dispatch(Context context) throws Exception {
        RpcRequest request = context.getRequest();
        Handler override = methods.get(request.getMethod());

        if (override != null)
            return override.handle(context);

        switch (request.getMethod()) {
            case "server/discover":
                Map<String, Object> caps = new LinkedHashMap<>();
                caps.put("tools", Collections.singletonMap("listChanged", true));
                caps.put("prompts", Collections.singletonMap("listChanged", true));
                Map<String, Object> resourceCaps = new LinkedHashMap<>();
                resourceCaps.put("listChanged", true);
                resourceCaps.put("subscribe", true);
                caps.put("resources", resourceCaps);
                caps.put("logging", Collections.emptyMap());
                if (methods.containsKey("completion/complete")) caps.put("completions", Collections.emptyMap());
                return Discovery.result(caps, name, version);
            case "tools/list":
                List<Object> list = new ArrayList<>();

                for (ToolEntry tool : tools.values()) {
                    if (context.getConnection().isHttp())
                        try {
                            HttpMetadata.validateTool(tool.getDefinition());
                        } catch (IllegalArgumentException ex) {
                            continue;
                        }

                    list.add(tool.getDefinition().toMap());
                }

                return listing(request, "tools", list);
            case "resources/list":
                return listing(request, "resources", descriptors(resources));
            case "prompts/list":
                return listing(request, "prompts", descriptors(prompts));
            case "resources/templates/list":
                return listing(request, "resourceTemplates", Collections.emptyList());
            case "tools/call":
                ToolEntry tool = tools.get(request.getParams().get("name"));

                if (tool == null)
                    throw invalid(request, "Unknown tool");

                tool.getContract().requireCall(request);
                McpResult result = tool.getHandler().handle(context);
                tool.getContract().requireResult(request, new RpcSuccess(request.getId(), result));

                return result;
            case "resources/read":
                Entry resource = resources.get(request.getParams().get("uri"));

                if (resource == null)
                    throw invalid(request, "Unknown resource");

                return resource.getHandler().handle(context);
            case "prompts/get":
                Entry prompt = prompts.get(request.getParams().get("name"));

                if (prompt == null)
                    throw invalid(request, "Unknown prompt");

                Object arguments = request.getParams().get("arguments");
                Object declarations = prompt.getDescriptor().get("arguments");

                if (declarations instanceof List) for (Object value : (List<?>) declarations) {
                    Map<?, ?> argument = (Map<?, ?>) value;

                    if (Boolean.TRUE.equals(argument.get("required")) && (!(arguments instanceof Map) || !((Map<?, ?>) arguments).containsKey(argument.get("name"))))
                        throw invalid(request, "Required prompt argument missing");
                }

                return prompt.getHandler().handle(context);
            default:
                throw new McpProtocolException(new RpcError(request.getId(), ErrorCodes.METHOD_NOT_FOUND, "Method not provided"), false, null);
        }
    }

    /**
     * Extracts discovery descriptors from one registry.
     *
     * @param entries resource or prompt registry
     * @return descriptors in registry order
     */
    private static List<Object> descriptors(Map<String, Entry> entries) {
        List<Object> result = new ArrayList<>();

        for (Entry entry : entries.values())
            result.add(entry.getDescriptor());

        return result;
    }

    /**
     * Creates the single-page result shape used by built-in registries.
     *
     * @param request list request, which must not carry a cursor
     * @param field   result field receiving the supplied values
     * @param values  values to expose
     * @return complete private-cache result
     */
    public static McpResult listing(RpcRequest request, String field, List<?> values) {
        // The built-in registry is a single page. Application handlers can implement opaque pagination.
        if (request.getParams().containsKey("cursor"))
            throw invalid(request, "Unknown cursor");

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(field, values);
        fields.put("ttlMs", 0);
        fields.put("cacheScope", "private");

        return McpResult.complete(fields);
    }

    /**
     * Deep-snapshots an open JSON object through the core immutable wire-value boundary.
     *
     * @param value JSON object to snapshot
     * @return immutable snapshot
     */
    private static Map<String, Object> snapshot(Map<String, Object> value) {
        return cast(McpResult.complete(Collections.singletonMap("value", value)).toMap().get("value"));
    }


    /**
     * Narrows a wire-validated JSON object snapshot.
     *
     * @param value validated object value
     * @return object map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    /**
     * Creates a correlated invalid-parameters error for a method-level validation failure.
     *
     * @param request offending request
     * @param message peer-safe explanation
     * @return protocol exception
     */
    private static McpProtocolException invalid(RpcRequest request, String message) {
        return new McpProtocolException(new RpcError(request.getId(), ErrorCodes.INVALID_PARAMS, message), false, null);
    }

    @Override
    public void report(Throwable ex) {
        try {
            errors.accept(ex);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delayMs) {
        return timer.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public Future<?> execute(Runnable task) {
        return workers.submit(task);
    }

    @Override
    public void subscriptionOpened(Context context) {
        subscriptions.add(context);
    }

    @Override
    public void subscriptionClosed(Context context) {
        subscriptions.remove(context);
    }

    @Override
    public void connectionClosed(Connection connection) {
        connections.remove(connection);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (Context context : subscriptions)
                if (!context.getConnection().isHttp())
                    context.emit(new RpcNotification("notifications/cancelled", Collections.singletonMap("requestId", context.getRequest().getId().getValue())));

            for (Connection connection : connections)
                connection.close();

            timer.shutdownNow();
            workers.shutdownNow();
        }
    }
}
