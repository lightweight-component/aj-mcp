package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.ProtocolVersion;
import com.ajaxjs.mcp.protocol.client.*;
import com.ajaxjs.mcp.protocol.common.Content;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.ajaxjs.mcp.protocol.resource.GetResourceListRequest;
import com.ajaxjs.mcp.protocol.tools.*;
import com.ajaxjs.mcp.protocol.utils.completion.CompleteRequest;
import com.ajaxjs.mcp.protocol.utils.completion.CompleteResult;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.protocol.utils.ping.PingResponse;
import com.ajaxjs.mcp.server.common.PaginatedResponse;
import com.ajaxjs.mcp.server.common.ServerUtils;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.feature.model.ServerStoreCompletion;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP Server Tools
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServer extends McpServerPrompt {
    private final ThreadLocal<String> currentSession = new ThreadLocal<>();
    private final Map<String, String> sessionProtocolVersions = new ConcurrentHashMap<>();
    private final Map<String, InitializeRequestParams.Capabilities> clientCapabilities = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> resourceSubscriptions = new ConcurrentHashMap<>();
    private volatile String loggingLevel = "info";
    /**
     * JSON-RPC request IDs are scoped to a connection. Keep the session in the
     * key so two clients using the same ID cannot cancel each other's tools.
     */
    private final Map<RequestKey, RunningRequest> runningRequests = new ConcurrentHashMap<>();
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();
    private final AtomicLong serverRequestIds = new AtomicLong(1);
    private final Map<String, CompletableFuture<JsonNode>> pendingClientResponses = new ConcurrentHashMap<>();

    private enum SessionState {NEW, INITIALIZING, READY}

    void bindSession(String sessionId) {
        currentSession.set(sessionId);
    }

    void clearSession() {
        currentSession.remove();
    }

    void removeSession(String sessionId) {
        resourceSubscriptions.forEach((uri, ignored) ->
                resourceSubscriptions.computeIfPresent(uri, (key, sessions) -> {
                    sessions.remove(sessionId);
                    return sessions.isEmpty() ? null : sessions;
                }));

        sessionStates.remove(sessionId);
        sessionProtocolVersions.remove(sessionId);
        clientCapabilities.remove(sessionId);
        runningRequests.forEach((key, request) -> {
            if (key.belongsTo(sessionId) && runningRequests.remove(key, request))
                request.cancel();
        });
        String prefix = sessionId + ":";
        pendingClientResponses.forEach((key, future) -> {
            if (key.startsWith(prefix) && pendingClientResponses.remove(key, future))
                future.completeExceptionally(new IllegalStateException("MCP session closed: " + sessionId));
        });
    }

    @Override
    protected void onProtocolNegotiated(String version, InitializeRequestParams requestParams) {
        String sessionId = currentSession.get() == null ? "direct" : currentSession.get();
        sessionProtocolVersions.put(sessionId, version);
        clientCapabilities.put(sessionId, requestParams.getCapabilities());
    }

    public String getNegotiatedProtocolVersion(String sessionId) {
        return sessionProtocolVersions.get(sessionId);
    }

    /**
     * Completes a pending server-to-client request response, if the JSON is a response envelope.
     */
    boolean acceptClientResponse(String sessionId, String rawJson) {
        JsonNode message = JsonUtils.json2Node(rawJson);
        if (message.has(METHOD) || !message.has(ID))
            return false;
        CompletableFuture<JsonNode> future = pendingClientResponses.remove(sessionId + ":" + message.get(ID).asText());
        if (future != null)
            future.complete(message);
        else
            log.warn("Received client response for unknown server request id {}", message.get(ID));
        return true;
    }

    public List<Root> listRoots(String sessionId, Duration timeout) {
        InitializeRequestParams.Capabilities capabilities = clientCapabilities.get(sessionId);
        if (capabilities == null || capabilities.getRoots() == null)
            throw new IllegalStateException("Client did not advertise roots capability");
        JsonNode result = requestClient(sessionId, Methods.ROOTS_LIST, null, timeout);
        List<Root> roots = new ArrayList<>();
        for (JsonNode root : result.path("roots"))
            roots.add(JsonUtils.OBJECT_MAPPER.convertValue(root, Root.class));
        return roots;
    }

    public SamplingCreateMessageResult createMessage(String sessionId, SamplingCreateMessageParams params,
                                                     Duration timeout) {
        InitializeRequestParams.Capabilities capabilities = clientCapabilities.get(sessionId);
        if (capabilities == null || capabilities.getSampling() == null)
            throw new IllegalStateException("Client did not advertise sampling capability");
        JsonNode result = requestClient(sessionId, Methods.SAMPLING_CREATE_MESSAGE,
                JsonUtils.OBJECT_MAPPER.valueToTree(params), timeout);
        return JsonUtils.OBJECT_MAPPER.convertValue(result, SamplingCreateMessageResult.class);
    }

    /**
     * Requests structured user input from a 2025-06-18 capable client.
     */
    public ElicitResult elicit(String sessionId, ElicitRequestParams params, Duration timeout) {
        String version = sessionProtocolVersions.get(sessionId);
        if (version == null || !ProtocolVersion.from(version).supportsElicitation())
            throw new IllegalStateException("Elicitation requires an MCP 2025-06-18 session");
        InitializeRequestParams.Capabilities capabilities = clientCapabilities.get(sessionId);
        if (capabilities == null || capabilities.getElicitation() == null)
            throw new IllegalStateException("Client did not advertise elicitation capability");
        JsonNode result = requestClient(sessionId, Methods.ELICITATION_CREATE,
                JsonUtils.OBJECT_MAPPER.valueToTree(params), timeout);
        return JsonUtils.OBJECT_MAPPER.convertValue(result, ElicitResult.class);
    }

    private JsonNode requestClient(String sessionId, String method, JsonNode params, Duration timeout) {
        long id = serverRequestIds.getAndIncrement();
        String key = sessionId + ":" + id;
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingClientResponses.put(key, future);
        ObjectNode request = JsonUtils.OBJECT_MAPPER.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put(ID, id);
        request.put(METHOD, method);

        if (params != null)
            request.set(PARAMS, params);

        try {
            transport.send(sessionId, request.toString());
            JsonNode response = timeout == null || timeout.isZero()
                    ? future.get() : future.get(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS);
            if (response.has("error"))
                throw new IllegalStateException("Client rejected " + method + ": " + response.get("error"));

            return response.path(RESPONSE_RESULT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for client response", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed waiting for client response to " + method, e);
        } finally {
            pendingClientResponses.remove(key, future);
        }
    }

    public void start() {
        log.info("MCP Server started, waiting for input...");
        transport.start();
    }

    /**
     * Handles received message requests.
     * Invokes the corresponding processing logic based on the method.
     *
     * @param requestRaw The raw request information, including the method, ID, and data
     * @return A response object depending on the requested method
     * @throws JsonRpcErrorException If the requested method is not found, this exception is thrown
     */
    public McpResponse processMessage(McpRequestRawInfo requestRaw) {
        if (requestRaw.getId() == null && !requestRaw.getMethod().startsWith("notifications/")) {
            log.warn("Ignoring MCP request method sent as a notification: {}", requestRaw.getMethod());
            return null;
        }

        String sessionId = currentSession.get() == null ? "direct" : currentSession.get();
        SessionState state = sessionStates.getOrDefault(sessionId, SessionState.NEW);
        boolean strict = serverConfig != null && serverConfig.isStrictLifecycle();

        if (strict && !Methods.INITIALIZE.equals(requestRaw.getMethod()) && !Methods.PING.equals(requestRaw.getMethod())
                && !Methods.NOTIFICATION_INITIALIZED.equals(requestRaw.getMethod()) && state != SessionState.READY)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_REQUEST,
                    "MCP session is not initialized");

        switch (requestRaw.getMethod()) {
            case Methods.INITIALIZE:
                if (strict && state != SessionState.NEW)
                    throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_REQUEST,
                            "MCP session is already initialized");

                JsonNode jsonNode = requestRaw.getJsonNode();
                McpResponse initialized = initialize(requestRaw.getId(), jsonNode);
                sessionStates.put(sessionId, SessionState.INITIALIZING);

                return initialized;
            case Methods.PING:
                PingResponse resp = new PingResponse();
                resp.setId(requestRaw.getId());

                return resp;
            case Methods.PROMPTS_LIST:
                return promptList(requestRaw);
            case Methods.PROMPTS_GET:
                return promptGet(requestRaw);
            case Methods.RESOURCES_LIST:
                return resourceList(requestRaw);
            case Methods.RESOURCES_READ:
                return resourceRead(requestRaw);
            case Methods.RESOURCES_TEMPLATES_LIST:
                return resourceTemplateList(requestRaw);
            case Methods.RESOURCES_SUBSCRIBE_REQUEST:
                return changeSubscription(requestRaw, true);
            case Methods.RESOURCES_UNSUBSCRIBE_REQUEST:
                return changeSubscription(requestRaw, false);
            case Methods.TOOLS_LIST:
                return toolList(requestRaw);
            case Methods.TOOLS_CALL:
                return toolCall(requestRaw);
            case Methods.COMPLETION_COMPLETE:
                return complete(requestRaw);
            case Methods.LOGGING_SET_LEVEL:
                return setLoggingLevel(requestRaw);
            case Methods.NOTIFICATION_INITIALIZED:
                if (strict && state != SessionState.INITIALIZING)
                    throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_REQUEST,
                            "Unexpected initialized notification");
                sessionStates.put(sessionId, SessionState.READY);
                return null;
            case Methods.NOTIFICATION_CANCELLED:
                cancelRequest(requestRaw);
                return null;
            default:
                if (requestRaw.getId() == null)
                    return null;
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method " + requestRaw.getMethod() + " not found.");
        }
    }

    private void cancelRequest(McpRequestRawInfo requestRaw) {
        JsonNode params = requestRaw.getJsonNode().get(PARAMS);
        JsonNode requestId = params == null ? null : params.get("requestId");

        if (requestId == null)
            return;

        try {
            RunningRequest request = runningRequests.get(requestKey(requestIdValue(requestId)));

            // Do not retain unknown cancellation IDs. JSON-RPC IDs may be reused,
            // so retaining one could cancel an unrelated future request.
            if (request != null)
                request.cancel();
        } catch (RuntimeException ignored) {
            log.debug("Ignoring malformed cancellation request id {}", requestId);
        }
    }

    private static Object requestIdValue(JsonNode requestId) {
        if (requestId.isIntegralNumber())
            return requestId.longValue();
        if (requestId.isTextual())
            return requestId.textValue();
        throw new IllegalArgumentException("Cancellation requestId must be a string or integer");
    }

    private RequestKey requestKey(Object requestId) {
        String sessionId = currentSession.get();
        return new RequestKey(sessionId == null ? "direct" : sessionId, requestId);
    }

    private McpResponse setLoggingLevel(McpRequestRawInfo requestRaw) {
        JsonNode params = requestRaw.getJsonNode().get(PARAMS);
        String level = params == null || params.get("level") == null ? null : params.get("level").asText();

        if (level == null || !java.util.Arrays.asList("debug", "info", "notice", "warning", "error", "critical", "alert", "emergency").contains(level))
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "invalid logging level");

        loggingLevel = level;
        McpResponse response = new McpResponse();
        response.setId(requestRaw.getId());
        response.setResult(Collections.emptyMap());

        return response;
    }

    public void publishToolsChanged() {
        broadcastNotification(Methods.TOOLS_LIST_CHANGED_NOTIFICATION, null);
    }

    public void publishPromptsChanged() {
        broadcastNotification(Methods.PROMPTS_LIST_CHANGED_NOTIFICATION, null);
    }

    public void publishResourcesChanged() {
        broadcastNotification(Methods.RESOURCE_LIST_CHANGED_NOTIFICATION, null);
    }

    public void sendProgress(String sessionId, Object progressToken, double progress, Double total) {
        ObjectNode params = JsonUtils.OBJECT_MAPPER.createObjectNode();
        params.set("progressToken", JsonUtils.OBJECT_MAPPER.valueToTree(progressToken));
        params.put("progress", progress);

        if (total != null)
            params.put("total", total);

        transport.send(sessionId, notificationJson(Methods.PROGRESS_NOTIFICATION, params));
    }

    /**
     * Sends progress to the session currently executing a request.
     */
    public void sendProgress(Object progressToken, double progress, Double total) {
        String sessionId = currentSession.get();

        if (sessionId == null)
            throw new IllegalStateException("No MCP request session is bound to this thread");

        sendProgress(sessionId, progressToken, progress, total);
    }

    public void publishLog(String level, String loggerName, Object data) {
        ObjectNode params = JsonUtils.OBJECT_MAPPER.createObjectNode();
        params.put("level", level == null ? loggingLevel : level);

        if (loggerName != null)
            params.put("logger", loggerName);
        params.set("data", JsonUtils.OBJECT_MAPPER.valueToTree(data));

        transport.broadcast(notificationJson(Methods.LOGGING_MESSAGE_NOTIFICATION, params));
    }

    private void broadcastNotification(String method, JsonNode params) {
        transport.broadcast(notificationJson(method, params));
    }

    private static String notificationJson(String method, JsonNode params) {
        ObjectNode notification = JsonUtils.OBJECT_MAPPER.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);

        if (params != null)
            notification.set(PARAMS, params);

        return notification.toString();
    }

    private McpResponse changeSubscription(McpRequestRawInfo requestRaw, boolean subscribe) {
        String sessionId = currentSession.get();

        if (sessionId == null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INTERNAL_ERROR, "Transport session is unavailable");

        JsonNode params = requestRaw.getJsonNode().get(PARAMS);
        String uri = params == null || params.get("uri") == null ? null : params.get("uri").asText();

        if (uri == null || uri.trim().isEmpty())
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "resource uri is required");

        if (subscribe)
            resourceSubscriptions.computeIfAbsent(uri, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        else {
            resourceSubscriptions.computeIfPresent(uri, (ignored, sessions) -> {
                sessions.remove(sessionId);
                return sessions.isEmpty() ? null : sessions;
            });
        }

        McpResponse response = new McpResponse();
        response.setId(requestRaw.getId());
        response.setResult(Collections.emptyMap());

        return response;
    }

    /**
     * Publishes a resource update only to sessions subscribed to the exact URI.
     */
    public void publishResourceUpdated(String uri) {
        Set<String> sessions = resourceSubscriptions.get(uri);

        if (sessions == null || sessions.isEmpty())
            return;

        com.ajaxjs.mcp.protocol.resource.SubscriptionUpdateNotification notification =
                new com.ajaxjs.mcp.protocol.resource.SubscriptionUpdateNotification();

        notification.setParams(new com.ajaxjs.mcp.protocol.resource.GetResourceRequest.Params(uri));
        String json = JsonUtils.toJson(notification);

        for (String sessionId : new ArrayList<>(sessions)) {
            try {
                transport.send(sessionId, json);
            } catch (RuntimeException e) {
                sessions.remove(sessionId);
                log.warn("Removing failed resource subscription for session {}", sessionId, e);
            }
        }
    }

    McpResponse complete(McpRequestRawInfo requestRaw) {
        JsonNode paramsNode = requestRaw.getJsonNode().get(PARAMS);
        if (paramsNode == null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "params is required");

        CompleteRequest.Params params = JsonUtils.jsonNode2bean(paramsNode, CompleteRequest.Params.class);

        if (params == null || params.getRef() == null || params.getArgument() == null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS,
                    "ref and argument are required");

        CompleteRequest.Ref ref = params.getRef();
        String reference = "ref/prompt".equals(ref.getType()) ? ref.getName() : resourceTemplateName(ref.getUri());
        String key = ref.getType() + ":" + reference + ":" + params.getArgument().getName();
        ServerStoreCompletion store = getStore(featureMgr.getCompletionStore(), key, requestRaw.getId(), "completion provider");
        Object returned;

        try {
            returned = store.getMethod().invoke(store.getInstance(), params.getArgument().getValue());
        } catch (IllegalAccessException e) {
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INTERNAL_ERROR,
                    "Completion method is not accessible", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INTERNAL_ERROR,
                    "Completion execution failed: " + cause.getMessage(), cause);
        }

        List<String> values = new ArrayList<>();

        if (returned instanceof List)
            for (Object value : (List<?>) returned)
                values.add(String.valueOf(value));
        else if (returned != null)
            values.add(String.valueOf(returned));

        int total = values.size();
        boolean hasMore = total > 100;

        if (hasMore)
            values = new ArrayList<>(values.subList(0, 100));

        CompleteResult response = new CompleteResult();
        response.setId(requestRaw.getId());
        CompleteResult.CompletionResult completion = new CompleteResult.CompletionResult(values, total, hasMore);
        response.setResult(new CompleteResult.CompleteResultDetail(completion));

        return response;
    }

    private String resourceTemplateName(String uriTemplate) {
        for (com.ajaxjs.mcp.server.feature.model.ServerStoreResourceTemplate store
                : featureMgr.getResourceTemplateStore().values())
            if (java.util.Objects.equals(uriTemplate, store.getResourceTemplate().getUriTemplate()))
                return store.getResourceTemplate().getName();

        return uriTemplate;
    }

    /**
     * Processes the tool list request from the client and returns the list of tools.
     * This method first checks if the request contains parameters, and if so, sets them.
     * Then it retrieves the list of tools from the resource store and constructs the response containing the list of tools.
     *
     * @param requestRaw The raw information of the client's request, containing the request ID and possibly parameters.
     * @return Returns the response object containing the list of tools.
     * @throws NullPointerException If the resource store is not initialized.
     */
    McpResponse toolList(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        GetResourceListRequest request = new GetResourceListRequest();
        request.setId(requestRaw.getId());

        if (jsonNode.has(PARAMS)) {
            JsonNode jsonNode1 = jsonNode.get(PARAMS);
            Cursor cursor = JsonUtils.jsonNode2bean(jsonNode1, Cursor.class);
            request.setParams(cursor);
        }

        List<ToolItem> tools = new ArrayList<>();

        String sessionId = currentSession.get() == null ? "direct" : currentSession.get();
        String negotiated = sessionProtocolVersions.get(sessionId);
        ProtocolVersion revision = negotiated == null ? ProtocolVersion.V_2024_11_05 : ProtocolVersion.from(negotiated);

        for (ServerStoreTool store : featureMgr.getToolStore().values()) {
            // Never mutate the shared feature definition while projecting it to
            // a session's negotiated schema.
            ToolItem tool = JsonUtils.OBJECT_MAPPER.convertValue(store.getTool(), ToolItem.class);
            if (revision == ProtocolVersion.V_2024_11_05)
                tool.setAnnotations(null);
            if (!revision.supportsStructuredToolOutput()) {
                tool.setTitle(null);
                tool.setOutputSchema(null);
            }
            tools.add(tool);
        }

        GetToolListResult result = new GetToolListResult();
        result.setId(requestRaw.getId());

        GetToolListResult.ToolList toolList;

        if (request.getParams() != null && request.getParams().getPageNo() != null) {
            // do the page
            PaginatedResponse<ToolItem> page = ServerUtils.paginate(tools, request.getParams(), this);
            tools = page.getList();
            toolList = new GetToolListResult.ToolList(tools);

            if (!page.isLastPage())
                toolList.setNextCursor(page.getNextPageNoAsBse64());
        } else
            toolList = new GetToolListResult.ToolList(tools);

        result.setResult(toolList);

        return result;
    }

    /**
     * Calls a tool using the provided request information.
     * <p>
     * This method processes the request to call a tool by parsing the request parameters,
     * validating them against the tool's input schema, and then invoking the tool's method
     * with the provided arguments. It handles errors such as invalid parameters and
     * runtime exceptions during method invocation.
     *
     * @param requestRaw The raw information of the tool call request, containing the necessary
     *                   information to make the call, such as parameters and request ID.
     * @return Returns a response object containing the result of the tool call.
     * @throws JsonRpcErrorException If the parameters are invalid or missing required arguments.
     * @throws RuntimeException      If an exception occurs during method invocation.
     */
    McpResponse toolCall(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        JsonNode paramsNode = jsonNode.get(PARAMS);

        if (paramsNode == null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "params is required");

        CallToolRequest.Params params = JsonUtils.jsonNode2bean(paramsNode, CallToolRequest.Params.class);
        Map<String, Object> arguments = params.getArguments();

        ServerStoreTool store = getStore(featureMgr.getToolStore(), params.getName(), requestRaw.getId(), "tool");
        ToolItem tool = store.getTool();
        JsonSchema inputSchema = tool.getInputSchema();

        Object[] argValues = null;

        if (inputSchema != null) {
            List<String> required = inputSchema.getRequired() == null ? Collections.emptyList() : inputSchema.getRequired();
            if ((arguments == null || arguments.isEmpty()) && !required.isEmpty())
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments is required!");

            if (arguments == null)
                arguments = Collections.emptyMap();

            if (arguments.size() < required.size())
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments size is not match!");

            Map<String, JsonSchemaProperty> argumentsDefined = inputSchema.getProperties();
            List<String> paramsOrder = store.getParamsOrder();
            argValues = new Object[paramsOrder.size()];

            if (argumentsDefined != null && !argumentsDefined.isEmpty()) {
                for (int i = 0; i < paramsOrder.size(); i++) {
                    String name = paramsOrder.get(i);
                    Object arg = arguments.get(name);

                    if (arg == null && required.contains(name))
                        throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments " + name + " is required!");

                    // Missing optional reference parameters are deliberately passed as null.
                    // Primitive parameters cannot represent absence and therefore remain required.
                    Class<?> parameterType = methodParameterType(store, i);
                    if (arg == null && parameterType.isPrimitive())
                        throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS,
                                "arguments " + name + " is required for primitive parameter");

                    argValues[i] = convertToType(arg, parameterType);
                }
            }
        }

        // executes tool method
        Method method = store.getMethod();
        Object returnedValue;

        RequestKey requestKey = requestKey(requestRaw.getId());
        RunningRequest runningRequest = new RunningRequest(Thread.currentThread());
        RunningRequest previous = runningRequests.putIfAbsent(requestKey, runningRequest);

        if (previous != null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_REQUEST,
                    "A request with the same id is already running in this session");

        try {
            if (argValues == null)
                returnedValue = method.invoke(store.getInstance());
            else
                returnedValue = method.invoke(store.getInstance(), argValues);
        } catch (IllegalAccessException e) {
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INTERNAL_ERROR,
                    "Tool method is not accessible: " + params.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            log.warn("Tool '{}' execution failed", params.getName(), cause);

            return toolErrorResult(requestRaw.getId(), cause);
        } finally {
            runningRequests.remove(requestKey, runningRequest);
            // Executor workers are reused; do not leak a cancellation interrupt into
            // an unrelated request accepted by the same worker later.
            Thread.interrupted();
        }

        List<Content> content;
        Map<String, Object> structuredContent = null;
        boolean structuredError = false;

        if (returnedValue instanceof StructuredToolResult) {
            String sessionId = currentSession.get() == null ? "direct" : currentSession.get();
            String version = sessionProtocolVersions.get(sessionId);
            if (version == null || !ProtocolVersion.from(version).supportsStructuredToolOutput())
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_REQUEST,
                        "Structured tool output requires MCP 2025-06-18");
            StructuredToolResult structured = (StructuredToolResult) returnedValue;
            try {
                content = toolContentList(requestRaw.getId(), structured.getContent());
            } catch (JsonRpcErrorException e) {
                return toolErrorResult(requestRaw.getId(), e);
            }
            structuredContent = structured.getStructuredContent();
            structuredError = structured.isError();
        } else if (returnedValue instanceof Content)
            content = Collections.singletonList((Content) returnedValue);
        else if (returnedValue instanceof String)
            content = Collections.singletonList(new ContentText((String) returnedValue));
        else if (returnedValue instanceof List) {
            try {
                content = toolContentList(requestRaw.getId(), (List<?>) returnedValue);
            } catch (JsonRpcErrorException e) {
                return toolErrorResult(requestRaw.getId(), e);
            }
        } else if (returnedValue == null)
            return toolErrorResult(requestRaw.getId(), new IllegalStateException("Tool returned null"));
        else
            content = Collections.singletonList(new ContentText(returnedValue.toString()));

        CallToolResult.CallToolResultDetail detail = new CallToolResult.CallToolResultDetail();
        detail.setContent(content);
        detail.setStructuredContent(structuredContent);
        detail.setIsError(structuredError);

        CallToolResult result = new CallToolResult();
        result.setId(requestRaw.getId());
        result.setResult(detail);

        return result;
    }

    private static List<Content> toolContentList(Object requestId, List<?> values) {
        if (values == null)
            throw new JsonRpcErrorException(requestId, JsonRpcErrorCode.INTERNAL_ERROR,
                    "Tool returned a null content list");

        List<Content> content = new ArrayList<>(values.size());
        for (Object value : values) {
            if (!(value instanceof Content))
                throw new JsonRpcErrorException(requestId, JsonRpcErrorCode.INTERNAL_ERROR,
                        "Tool content list contains an unsupported value: "
                                + (value == null ? "null" : value.getClass().getName()));
            content.add((Content) value);
        }
        return content;
    }

    private static Class<?> methodParameterType(ServerStoreTool store, int index) {
        return store.getMethod().getParameterTypes()[index];
    }

    /**
     * Converts JSON-compatible values to the exact Java reflection parameter type.
     * Keeping conversion close to the invocation boundary avoids schema spelling
     * differences such as "number" versus "Number" from affecting execution.
     */
    public static Object convertToType(Object value, Class<?> targetType) {
        if (value == null)
            return null;
        if (targetType.isInstance(value))
            return value;
        if (targetType == String.class || targetType == char.class || targetType == Character.class)
            return value.toString();
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean)
                return value;
            String booleanValue = value.toString();
            if ("true".equalsIgnoreCase(booleanValue) || "false".equalsIgnoreCase(booleanValue))
                return Boolean.valueOf(booleanValue);
            throw new IllegalArgumentException("Value cannot be converted to Boolean: " + value);
        }
        if (targetType == byte.class || targetType == Byte.class)
            return number(value).byteValue();
        if (targetType == short.class || targetType == Short.class)
            return number(value).shortValue();
        if (targetType == int.class || targetType == Integer.class)
            return number(value).intValue();
        if (targetType == long.class || targetType == Long.class)
            return number(value).longValue();
        if (targetType == float.class || targetType == Float.class)
            return number(value).floatValue();
        if (targetType == double.class || targetType == Double.class)
            return number(value).doubleValue();

        return JsonUtils.OBJECT_MAPPER.convertValue(value, targetType);
    }

    private static Number number(Object value) {
        if (value instanceof Number)
            return (Number) value;
        String text = value.toString();
        try {
            return text.indexOf('.') >= 0 ? Double.valueOf(text) : Long.valueOf(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value cannot be converted to Number: " + value, e);
        }
    }

    private static final class RequestKey {
        private final String sessionId;
        private final Object requestId;

        private RequestKey(String sessionId, Object requestId) {
            this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
            this.requestId = Objects.requireNonNull(requestId, "requestId");
        }

        private boolean belongsTo(String sessionId) {
            return this.sessionId.equals(sessionId);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof RequestKey))
                return false;
            RequestKey that = (RequestKey) other;
            return sessionId.equals(that.sessionId) && requestId.equals(that.requestId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionId, requestId);
        }
    }

    private static final class RunningRequest {
        private final Thread thread;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private RunningRequest(Thread thread) {
            this.thread = thread;
        }

        private void cancel() {
            if (cancelled.compareAndSet(false, true))
                thread.interrupt();
        }
    }

    private static McpResponse toolErrorResult(Object requestId, Throwable cause) {
        String message = cause.getMessage();
        if (message == null || message.trim().isEmpty())
            message = cause.getClass().getSimpleName();

        CallToolResult.CallToolResultDetail detail = new CallToolResult.CallToolResultDetail();
        detail.setIsError(true);
        detail.setContent(Collections.singletonList(new ContentText(message)));

        CallToolResult result = new CallToolResult();
        result.setId(requestId);
        result.setResult(detail);

        return result;
    }

    /**
     * Converts the given value to the target type.
     *
     * @param value      The value to be converted, must not be null
     * @param targetType The target type, supported types include: "string", "number", "boolean"
     * @return The converted value
     * @throws IllegalArgumentException if value or targetType is null, or if the conversion is not possible
     */
    public static Object convertToType(Object value, String targetType) {
        if (value == null || targetType == null)
            throw new IllegalArgumentException("Value or targetType cannot be null");

        switch (targetType.toLowerCase()) {
            case "string":
                return value.toString();
            case "number":
                // 首先检查是否是数字类型
                if (value instanceof Number) {
                    Number numberValue = (Number) value;

                    if (numberValue.doubleValue() == numberValue.intValue()) {
                        // 如果没有小数部分，判断为整数
                        if (numberValue.longValue() == numberValue.intValue()) {
                            // 如果值在 int 范围内，返回 int
                            return numberValue.intValue();
                        } else
                            // 否则返回 long
                            return numberValue.longValue();
                    } else
                        // 如果有小数部分，返回 double
                        return numberValue.doubleValue();
                }

                // 如果是字符串值，尝试解析成数字
                try {
                    String strValue = value.toString();
                    if (strValue.contains("."))  // 判断为小数，返回 double
                        return Double.parseDouble(strValue);
                    else {
                        // 判断为整数
                        long longValue = Long.parseLong(strValue);

                        if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE)
                            return (int) longValue; // 如果值在 int 范围内，返回 int
                        else
                            return longValue;  // 否则返回 long
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value cannot be converted to Number: " + value);
                }
            case "boolean":
                if (value instanceof Boolean)
                    return value; // 如果已经是布尔值，直接返回

                String strValue = value.toString().toLowerCase();
                if ("true".equals(strValue) || "false".equals(strValue))
                    return Boolean.parseBoolean(strValue);

                throw new IllegalArgumentException("Value cannot be converted to Boolean: " + value);

            default:
                throw new IllegalArgumentException("Unsupported targetType: " + targetType);
        }
    }

}
