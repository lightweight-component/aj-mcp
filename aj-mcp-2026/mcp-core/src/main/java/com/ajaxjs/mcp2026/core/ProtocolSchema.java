package com.ajaxjs.mcp2026.core;

import com.ajaxjs.mcp2026.core.json.McpJacksonEngine;
import com.ajaxjs.mcp2026.core.schema.DefaultSchemaValidator;
import com.ajaxjs.mcp2026.core.schema.SchemaValidator;
import com.ajaxjs.mcp2026.core.schema.SchemaViolation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explicit method-level validation against the bundled, version-pinned MCP schema.
 * The envelope codec stays independent so extensions can use their own validators.
 */
public final class ProtocolSchema {
    /**
     * Immutable bundled schema definitions indexed by their official definition names.
     */
    private final Map<String, Object> definitions;
    /**
     * Schema compiler used to produce lazy reusable validators for bundled definitions.
     */
    private final SchemaValidator validator;
    /**
     * Compiled definition validators cached per schema type for this protocol instance.
     */
    private final Map<String, SchemaValidator.CompiledSchema> compiled = new ConcurrentHashMap<>();
    /**
     * Mapping from supported core request methods to their bundled schema definition names.
     */
    private final Map<String, String> requests = new LinkedHashMap<>();
    /**
     * Mapping from supported core notification methods to their bundled schema definition names.
     */
    private final Map<String, String> notifications = new LinkedHashMap<>();

    /**
     * Loads the bundled protocol schema using the default offline JSON Schema validator.
     */
    public ProtocolSchema() {
        this(new DefaultSchemaValidator());
    }

    /**
     * Loads the bundled protocol schema and uses the supplied validator for all lazy compiled definitions.
     *
     * @param validator schema validator implementation bound to this protocol instance
     * @throws IllegalStateException if the bundled schema resource cannot be loaded
     */
    public ProtocolSchema(SchemaValidator validator) {
        this.validator = java.util.Objects.requireNonNull(validator, "validator");
        try (InputStream stream = ProtocolSchema.class.getResourceAsStream("schema-2026-07-28.json")) {
            if (stream == null)
                throw new IllegalStateException("Bundled MCP schema missing");
            Map<?, ?> root = new McpJacksonEngine().OBJECT_MAPPER.readValue(stream, Map.class);
            definitions = WireValues.object(root.get("$defs"), "$defs");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load bundled MCP schema", e);
        }
        for (String type : new String[]{"DiscoverRequest", "ListResourcesRequest", "ListResourceTemplatesRequest",
                "ReadResourceRequest", "SubscriptionsListenRequest", "ListPromptsRequest", "GetPromptRequest",
                "ListToolsRequest", "CallToolRequest", "CompleteRequest"})
            requests.put(method(type), type);
        for (String type : new String[]{"CancelledNotification", "ProgressNotification", "ResourceListChangedNotification",
                "SubscriptionsAcknowledgedNotification", "ResourceUpdatedNotification", "PromptListChangedNotification",
                "ToolListChangedNotification", "LoggingMessageNotification"})
            notifications.put(method(type), type);
    }

    /**
     * Reads a protocol method constant from a named bundled schema definition.
     *
     * @param type definition name that declares a constant {@code method} property
     * @return MCP method string defined by the schema
     * @throws IllegalArgumentException if the bundled definition has an unexpected shape
     */
    private String method(String type) {
        Map<String, Object> definition = WireValues.object(definitions.get(type), type);
        Map<String, Object> properties = WireValues.object(definition.get("properties"), "properties");
        return WireValues.string(WireValues.object(properties.get("method"), "method").get("const"), "method.const");
    }

    /**
     * Validates any named definition, including content blocks and MRTR inputs.
     *
     * @param type  bundled schema definition name
     * @param value JSON-compatible value to validate
     * @return immutable violation list; empty only when the value satisfies the definition
     * @throws IllegalArgumentException if {@code type} is not present in the bundled schema
     */
    public List<SchemaViolation> validate(String type, Object value) {
        if (!definitions.containsKey(type))
            throw new IllegalArgumentException("Unknown protocol schema type");
        return compiled.computeIfAbsent(type, key -> {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("$schema", SchemaValidator.DEFAULT_DIALECT);
            schema.put("$defs", definitions);
            schema.put("$ref", "#/$defs/" + key);
            return validator.compile(schema);
        }).validate(value);
    }

    /**
     * Required validation for known core client requests before server dispatch.
     *
     * @param request decoded request to validate
     * @throws McpProtocolException if the version, method, or method parameters are invalid
     */
    public void requireRequest(RpcRequest request) {
        ProtocolValidator.requireSupportedVersion(request);
        String type = requests.get(request.getMethod());
        if (type == null)
            throw failure(request.getId(), false, ErrorCodes.METHOD_NOT_FOUND, "Unknown core method");
        if (!validate(type, request.toMap()).isEmpty())
            throw failure(request.getId(), false, ErrorCodes.INVALID_PARAMS, "Invalid method parameters");
    }

    /**
     * Validates core notification shape; failures must never be serialized as responses.
     *
     * @param notification decoded core notification to validate
     * @throws McpProtocolException if the notification method or parameters are invalid
     */
    public void requireNotification(RpcNotification notification) {
        String type = notifications.get(notification.getMethod());
        if (type == null || !validate(type, notification.toMap()).isEmpty())
            throw failure(null, true, ErrorCodes.INVALID_PARAMS, "Invalid core notification");
    }

    /**
     * Client-to-server core notifications are cancellation only.
     *
     * @param notification notification received from a client
     * @throws McpProtocolException if it is not a valid cancellation notification
     */
    public void requireClientNotification(RpcNotification notification) {
        if (!"notifications/cancelled".equals(notification.getMethod()))
            throw failure(null, true, ErrorCodes.INVALID_PARAMS, "Invalid client notification method");
        requireNotification(notification);
    }

    /**
     * Stateless checks for notifications tied to a known in-flight request.
     *
     * @param request      request that opted in to the notification
     * @param notification candidate request-scoped notification
     * @throws IllegalArgumentException if the notification is invalid or not permitted by request metadata
     */
    public void requireRequestNotification(RpcRequest request, RpcNotification notification) {
        requireNotification(notification);
        Map<String, Object> params = notification.getParams();
        String method = notification.getMethod();
        if ("notifications/progress".equals(method)) {
            Object token = request.getMetadata().toMap().get("progressToken");
            Object supplied = params.get("progressToken");
            boolean equal = token instanceof Number && supplied instanceof Number
                    ? new java.math.BigDecimal(token.toString()).compareTo(new java.math.BigDecimal(supplied.toString())) == 0
                    : java.util.Objects.equals(token, supplied);
            if (token != null && equal)
                return;
        } else if ("notifications/message".equals(method)) {
            List<String> levels = java.util.Arrays.asList("debug", "info", "notice", "warning", "error", "critical", "alert", "emergency");
            int threshold = levels.indexOf(request.getMetadata().toMap().get("io.modelcontextprotocol/logLevel"));
            int actual = levels.indexOf(params.get("level"));
            if (threshold >= 0 && actual >= threshold)
                return;
        } else if ("notifications/cancelled".equals(method) && Subscriptions.LISTEN.equals(request.getMethod())
                && request.getId().equals(RequestId.from(params.get("requestId")))) {
            // Server cancellation is only for a listen request on STDIO. The
            // transport must reject this branch for HTTP, which closes its stream.
            return;
        }
        throw new IllegalArgumentException("Notification is not permitted for this request");
    }

    /**
     * Response validation failures are local: clients must not reply with an error.
     *
     * @param request  originating request used for correlation and result-type selection
     * @param response candidate success response
     * @throws IllegalArgumentException if correlation, result shape, or capabilities are invalid
     */
    public void requireResult(RpcRequest request, RpcSuccess response) {
        if (!request.getId().equals(response.getId()))
            throw new IllegalArgumentException("Response ID mismatch");
        McpResult result = response.getResult();
        if (McpResult.INPUT_REQUIRED.equals(result.getResultType())) {
            if (!MultiRoundTrip.supports(request.getMethod()))
                throw new IllegalArgumentException("Method cannot return input_required");
            requireType("InputRequiredResult", result.toMap());
            requireInputCapabilities(request, result);
            return;
        }
        if (!McpResult.COMPLETE.equals(result.getResultType()))
            throw new IllegalArgumentException("Extension result needs an extension validator");
        String requestType = requests.get(request.getMethod());
        if (requestType == null)
            throw new IllegalArgumentException("Unknown core method");
        String type = requestType.substring(0, requestType.length() - "Request".length()) + "Result";
        requireType(type, result.toMap());
        if (Subscriptions.LISTEN.equals(request.getMethod()))
            Subscriptions.validateClosed(request, response);
    }

    /**
     * Throws a local validation exception without echoing potentially sensitive payloads.
     *
     * @param type  bundled schema definition name
     * @param value candidate JSON-compatible value
     * @throws IllegalArgumentException if the value violates the named definition
     */
    public void requireType(String type, Object value) {
        if (!validate(type, value).isEmpty())
            throw new IllegalArgumentException("Invalid " + type);
    }

    /**
     * Checks a client-built continuation, including each input's corresponding result type.
     *
     * @param original      original request before the input-required result
     * @param inputRequired correlated response requesting additional input
     * @param retry         candidate independent continuation request
     * @throws IllegalArgumentException if state, input responses, schemas, or immutable request fields differ
     */
    public void requireRetry(RpcRequest original, RpcSuccess inputRequired, RpcRequest retry) {
        requireResult(original, inputRequired);
        if (!McpResult.INPUT_REQUIRED.equals(inputRequired.getResult().getResultType()))
            throw new IllegalArgumentException("Expected input_required");
        Map<String, Object> responses = retry.getParams().containsKey("inputResponses")
                ? WireValues.object(retry.getParams().get("inputResponses"), "inputResponses") : Collections.emptyMap();
        RpcRequest expected = MultiRoundTrip.retry(original, inputRequired, retry.getId(), responses);
        if (!expected.getMethod().equals(retry.getMethod()) || !expected.getParams().equals(retry.getParams()))
            throw new IllegalArgumentException("Continuation changed original parameters or opaque state");
        requireRequest(retry);
        Object requested = inputRequired.getResult().toMap().get("inputRequests");
        if (requested == null)
            return;
        for (Map.Entry<String, Object> entry : WireValues.object(requested, "inputRequests").entrySet()) {
            Map<String, Object> input = WireValues.object(entry.getValue(), "inputRequest");
            Map<String, Object> value = WireValues.object(responses.get(entry.getKey()), "inputResponse");
            String method = (String) input.get("method");
            String type = "roots/list".equals(method) ? "ListRootsResult"
                    : "sampling/createMessage".equals(method) ? "CreateMessageResult" : "ElicitResult";
            requireType(type, value);
            if ("elicitation/create".equals(method) && "accept".equals(value.get("action"))) {
                Map<String, Object> params = WireValues.object(input.get("params"), "params");
                if (!"url".equals(params.get("mode"))) {
                    if (!value.containsKey("content") || !validator.compile(
                                    WireValues.object(params.get("requestedSchema"), "requestedSchema"))
                            .validate(value.get("content")).isEmpty())
                        throw new IllegalArgumentException("Elicitation content does not satisfy requestedSchema");
                }
            }
        }
    }

    /**
     * Validates a core subscription notification against the accepted filter.
     * Callers still enforce acknowledgment-first ordering and active-stream lifetime.
     *
     * @param listen       original subscription listen request
     * @param acknowledged correlated server acknowledgement
     * @param notification subsequent candidate subscription notification
     * @throws IllegalArgumentException if the notification is not included in the accepted filter
     */
    public void requireSubscriptionNotification(RpcRequest listen, RpcNotification acknowledged, RpcNotification notification) {
        requireSubscriptionNotification(listen, acknowledged, notification, String::equals);
    }

    /**
     * Allows a resource provider to define sub-resource matching without unsafe URI prefix guesses.
     *
     * @param listen          original subscription listen request
     * @param acknowledged    correlated server acknowledgement
     * @param notification    subsequent candidate subscription notification
     * @param resourceMatches application-specific predicate for requested and updated resource URIs
     * @throws IllegalArgumentException if correlation, schemas, or accepted-filter checks fail
     */
    public void requireSubscriptionNotification(RpcRequest listen, RpcNotification acknowledged, RpcNotification notification,
                                                java.util.function.BiPredicate<String, String> resourceMatches) {
        java.util.Objects.requireNonNull(resourceMatches, "resourceMatches");
        requireRequest(listen);
        if (!Subscriptions.LISTEN.equals(listen.getMethod()) || !Subscriptions.ACKNOWLEDGED.equals(acknowledged.getMethod()))
            throw new IllegalArgumentException("Expected listen and acknowledgment");
        requireNotification(acknowledged);
        if (!listen.getId().equals(Subscriptions.notificationId(acknowledged)))
            throw new IllegalArgumentException("Acknowledgment ID mismatch");
        Map<String, Object> filter = Subscriptions.filter(acknowledged.getParams().get("notifications"));
        Subscriptions.acknowledge(listen, filter); // Checks the accepted subset.
        requireNotification(notification);
        if (!listen.getId().equals(Subscriptions.notificationId(notification)))
            throw new IllegalArgumentException("Subscription ID mismatch");
        String method = notification.getMethod();
        String flag = "notifications/tools/list_changed".equals(method) ? "toolsListChanged"
                : "notifications/prompts/list_changed".equals(method) ? "promptsListChanged"
                : "notifications/resources/list_changed".equals(method) ? "resourcesListChanged" : null;
        if (flag != null && Boolean.TRUE.equals(filter.get(flag)))
            return;
        if ("notifications/resources/updated".equals(method) && filter.containsKey("resourceSubscriptions")) {
            for (Object uri : (List<?>) filter.get("resourceSubscriptions"))
                if (resourceMatches.test((String) uri, (String) notification.getParams().get("uri")))
                    return;
        }
        throw new IllegalArgumentException("Notification was not accepted on this subscription");
    }

    /**
     * Derives capabilities required by input requests and verifies that the original client advertised them.
     *
     * @param request originating client request whose metadata contains declared capabilities
     * @param result  input-required result containing zero or more requested input operations
     * @throws IllegalArgumentException if input requests exceed the client's advertised capabilities
     */
    private void requireInputCapabilities(RpcRequest request, McpResult result) {
        if (!result.toMap().containsKey("inputRequests"))
            return;
        Map<String, Object> inputs = WireValues.object(result.toMap().get("inputRequests"), "inputRequests");
        for (Object value : inputs.values()) {
            Map<String, Object> input = WireValues.object(value, "inputRequest");
            String method = (String) input.get("method");
            Map<String, Object> params = input.containsKey("params")
                    ? WireValues.object(input.get("params"), "params") : Collections.emptyMap();
            Map<String, Object> required = new LinkedHashMap<>();
            if ("elicitation/create".equals(method)) {
                String mode = params.containsKey("mode") ? (String) params.get("mode") : "form";
                required.put("elicitation", Collections.singletonMap(mode, Collections.emptyMap()));
            } else if ("sampling/createMessage".equals(method)) {
                Map<String, Object> sampling = new LinkedHashMap<>();
                if (params.containsKey("tools") || params.containsKey("toolChoice"))
                    sampling.put("tools", Collections.emptyMap());
                if (params.containsKey("includeContext") && !"none".equals(params.get("includeContext")))
                    sampling.put("context", Collections.emptyMap());
                required.put("sampling", sampling);
            } else if ("roots/list".equals(method)) {
                required.put("roots", Collections.emptyMap());
            }
            try {
                ProtocolValidator.requireCapabilities(request, required);
            } catch (McpProtocolException e) {
                // This is an invalid response from a peer, not a request to answer.
                throw new IllegalArgumentException("Input request exceeds client capabilities", e);
            }
        }
    }

    /**
     * Creates a protocol failure while preserving notification response-suppression semantics.
     *
     * @param id           correlated request identifier, or {@code null} for an uncorrelated notification
     * @param notification whether the invalid input was a notification
     * @param code         JSON-RPC or MCP protocol error code
     * @param message      peer-safe error text
     * @return exception carrying a serializable error envelope when appropriate
     */
    private static McpProtocolException failure(RequestId id, boolean notification, int code, String message) {
        return new McpProtocolException(new RpcError(id, code, message), notification, null);
    }
}
