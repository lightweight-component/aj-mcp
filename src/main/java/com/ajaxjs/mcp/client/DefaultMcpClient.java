package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.McpUtils;
import com.ajaxjs.mcp.client.logging.DefaultMcpLogMessageHandler;
import com.ajaxjs.mcp.client.logging.McpLogMessageHandler;
import com.ajaxjs.mcp.client.protocol.*;
import com.ajaxjs.mcp.client.transport.McpOperationHandler;
import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.prompt.GetPromptResult;
import com.ajaxjs.mcp.prompt.Prompt;
import com.ajaxjs.mcp.prompt.PromptsHelper;
import com.ajaxjs.mcp.resource.ReadResourceResult;
import com.ajaxjs.mcp.resource.Resource;
import com.ajaxjs.mcp.resource.ResourceTemplate;
import com.ajaxjs.mcp.resource.ResourcesHelper;
import com.ajaxjs.mcp.tool.ToolExecutionHelper;
import com.ajaxjs.mcp.tool.ToolExecutionRequest;
import com.ajaxjs.mcp.tool.ToolSpecification;
import com.ajaxjs.mcp.tool.ToolSpecificationHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

// TODO: currently we request a new list of tools every time, so we should
// add support for the `ToolListChangedNotification` message, and then we can
// cache the list

@Slf4j
public class DefaultMcpClient implements McpClient {
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final McpTransport transport;
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String clientName;
    private final String clientVersion;
    private final String protocolVersion;
    private final Duration toolExecutionTimeout;
    private final Duration resourcesTimeout;
    private final Duration promptsTimeout;
    private final JsonNode RESULT_TIMEOUT;
    private final String toolExecutionTimeoutErrorMessage;
    private final Map<Long, CompletableFuture<JsonNode>> pendingOperations = new ConcurrentHashMap<>();
    private final McpOperationHandler messageHandler;
    private final McpLogMessageHandler logHandler;
    private final AtomicReference<List<Resource>> resourceRefs = new AtomicReference<>();
    private final AtomicReference<List<ResourceTemplate>> resourceTemplateRefs = new AtomicReference<>();
    private final AtomicReference<List<Prompt>> promptRefs = new AtomicReference<>();

    public DefaultMcpClient(Builder builder) {
        transport = Objects.requireNonNull(builder.transport, "transport");
        clientName = McpUtils.getOrDefault(builder.clientName, "langchain4j");
        clientVersion = McpUtils.getOrDefault(builder.clientVersion, "1.0");
        protocolVersion = McpUtils.getOrDefault(builder.protocolVersion, "2024-11-05");
        toolExecutionTimeout = McpUtils.getOrDefault(builder.toolExecutionTimeout, Duration.ofSeconds(60));
        resourcesTimeout = McpUtils.getOrDefault(builder.resourcesTimeout, Duration.ofSeconds(60));
        promptsTimeout = McpUtils.getOrDefault(builder.promptsTimeout, Duration.ofSeconds(60));
        logHandler = McpUtils.getOrDefault(builder.logHandler, new DefaultMcpLogMessageHandler());
        toolExecutionTimeoutErrorMessage = McpUtils.getOrDefault(builder.toolExecutionTimeoutErrorMessage, "There was a timeout executing the tool");
        RESULT_TIMEOUT = JsonNodeFactory.instance.objectNode();
        messageHandler = new McpOperationHandler(pendingOperations, transport, logHandler::handleLogMessage);
        ((ObjectNode) RESULT_TIMEOUT)
                .putObject("result")
                .putArray("content")
                .addObject()
                .put("type", "text")
                .put("text", toolExecutionTimeoutErrorMessage);
        initialize();
    }

    private void initialize() {
        transport.start(messageHandler);
        long operationId = idGenerator.getAndIncrement();
        InitializeRequest request = new InitializeRequest(operationId);
        InitializeParams params = createInitializeParams();
        request.setParams(params);

        try {
            JsonNode capabilities = transport.initialize(request).get();
            log.debug("MCP server capabilities: {}", capabilities.get("result"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    private InitializeParams createInitializeParams() {
        InitializeParams params = new InitializeParams();
        params.setProtocolVersion(protocolVersion);

        InitializeParams.ClientInfo clientInfo = new InitializeParams.ClientInfo();
        clientInfo.setName(clientName);
        clientInfo.setVersion(clientVersion);
        params.setClientInfo(clientInfo);

        InitializeParams.Capabilities capabilities = new InitializeParams.Capabilities();
        InitializeParams.Capabilities.Roots roots = new InitializeParams.Capabilities.Roots();
        roots.setListChanged(false); // TODO: listChanged is not supported yet
        capabilities.setRoots(roots);
        params.setCapabilities(capabilities);

        return params;
    }

    @Override
    public List<ToolSpecification> listTools() {
        ListToolsRequest operation = new ListToolsRequest(idGenerator.getAndIncrement());
        CompletableFuture<JsonNode> resultFuture = transport.executeOperationWithResponse(operation);
        JsonNode result = null;
        try {
            result = resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }

        return ToolSpecificationHelper.toolSpecificationListFromMcpResponse((ArrayNode) result.get("result").get("tools"));
    }

    @Override
    public String executeTool(ToolExecutionRequest executionRequest) {
        ObjectNode arguments;

        try {
            arguments = OBJECT_MAPPER.readValue(executionRequest.getArguments(), ObjectNode.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        long operationId = idGenerator.getAndIncrement();
        CallToolRequest operation = new CallToolRequest(operationId, executionRequest.getName(), arguments);
        long timeoutMillis = toolExecutionTimeout.toMillis() == 0 ? Integer.MAX_VALUE : toolExecutionTimeout.toMillis();
        CompletableFuture<JsonNode> resultFuture;
        JsonNode result;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            transport.executeOperationWithoutResponse(new CancellationNotification(operationId, "Timeout"));
            return ToolExecutionHelper.extractResult(RESULT_TIMEOUT);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
        return ToolExecutionHelper.extractResult(result);
    }

    @Override
    public List<Resource> listResources() {
        if (resourceRefs.get() == null)
            obtainResourceList();

        return resourceRefs.get();
    }

    @Override
    public ReadResourceResult readResource(String uri) {
        long operationId = idGenerator.getAndIncrement();
        ReadResourceRequest operation = new ReadResourceRequest(operationId, uri);
        long timeoutMillis = resourcesTimeout.toMillis() == 0 ? Integer.MAX_VALUE : resourcesTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return ResourcesHelper.parseResourceContents(result);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    @Override
    public List<Prompt> listPrompts() {
        if (promptRefs.get() == null)
            obtainPromptList();

        return promptRefs.get();
    }

    @Override
    public GetPromptResult getPrompt(String name, Map<String, Object> arguments) {
        long operationId = idGenerator.getAndIncrement();
        GetPromptRequest operation = new GetPromptRequest(operationId, name, arguments);
        long timeoutMillis = promptsTimeout.toMillis() == 0 ? Integer.MAX_VALUE : promptsTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);

            return PromptsHelper.parsePromptContents(result);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    @Override
    public List<ResourceTemplate> listResourceTemplates() {
        if (resourceTemplateRefs.get() == null)
            obtainResourceTemplateList();

        return resourceTemplateRefs.get();
    }

    private synchronized void obtainResourceList() {
        if (resourceRefs.get() != null)
            return;

        ListResourcesRequest operation = new ListResourcesRequest(idGenerator.getAndIncrement());
        long timeoutMillis = resourcesTimeout.toMillis() == 0 ? Integer.MAX_VALUE : resourcesTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            resourceRefs.set(ResourcesHelper.parseResourceRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }
    }

    private synchronized void obtainResourceTemplateList() {
        if (resourceTemplateRefs.get() != null)
            return;

        ListResourceTemplatesRequest operation = new ListResourceTemplatesRequest(idGenerator.getAndIncrement());
        long timeoutMillis = toolExecutionTimeout.toMillis() == 0 ? Integer.MAX_VALUE : toolExecutionTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            resourceTemplateRefs.set(ResourcesHelper.parseResourceTemplateRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }
    }

    private synchronized void obtainPromptList() {
        if (promptRefs.get() != null) {
            return;
        }
        ListPromptsRequest operation = new ListPromptsRequest(idGenerator.getAndIncrement());
        long timeoutMillis = promptsTimeout.toMillis() == 0 ? Integer.MAX_VALUE : promptsTimeout.toMillis();
        JsonNode result = null;
        CompletableFuture<JsonNode> resultFuture = null;
        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            promptRefs.set(PromptsHelper.parsePromptRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }
    }

    @Override
    public void close() {
        try {
            transport.close();
        } catch (Exception e) {
            log.warn("Cannot close MCP transport", e);
        }
    }

    public static class Builder {

        private String toolExecutionTimeoutErrorMessage;
        private McpTransport transport;
        private String clientName;
        private String clientVersion;
        private String protocolVersion;
        private Duration toolExecutionTimeout;
        private Duration resourcesTimeout;
        private Duration promptsTimeout;
        private McpLogMessageHandler logHandler;

        public Builder transport(McpTransport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * Sets the name that the client will use to identify itself to the
         * MCP server in the initialization message. The default value is
         * "langchain4j".
         */
        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        /**
         * Sets the version string that the client will use to identify
         * itself to the MCP server in the initialization message. The
         * default value is "1.0".
         */
        public Builder clientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        /**
         * Sets the protocol version that the client will advertise in the
         * initialization message. The default value right now is
         * "2024-11-05", but will change over time in later langchain4j
         * versions.
         */
        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        /**
         * Sets the timeout for tool execution.
         * This value applies to each tool execution individually.
         * The default value is 60 seconds.
         * A value of zero means no timeout.
         */
        public Builder toolExecutionTimeout(Duration toolExecutionTimeout) {
            this.toolExecutionTimeout = toolExecutionTimeout;
            return this;
        }

        /**
         * Sets the timeout for resource-related operations (listing resources as well as reading the contents of a resource).
         * The default value is 60 seconds.
         * A value of zero means no timeout.
         */
        public Builder resourcesTimeout(Duration resourcesTimeout) {
            this.resourcesTimeout = resourcesTimeout;
            return this;
        }

        /**
         * Sets the timeout for prompt-related operations (listing prompts as well as rendering the contents of a prompt).
         * The default value is 60 seconds.
         * A value of zero means no timeout.
         */
        public Builder promptsTimeout(Duration promptsTimeout) {
            this.promptsTimeout = promptsTimeout;
            return this;
        }

        /**
         * Sets the error message to return when a tool execution times out.
         * The default value is "There was a timeout executing the tool".
         */
        public Builder toolExecutionTimeoutErrorMessage(String toolExecutionTimeoutErrorMessage) {
            this.toolExecutionTimeoutErrorMessage = toolExecutionTimeoutErrorMessage;
            return this;
        }

        /**
         * Sets the log message handler for the client.
         */
        public Builder logHandler(McpLogMessageHandler logHandler) {
            this.logHandler = logHandler;
            return this;
        }

        public DefaultMcpClient build() {
            return new DefaultMcpClient(this);
        }
    }
}
