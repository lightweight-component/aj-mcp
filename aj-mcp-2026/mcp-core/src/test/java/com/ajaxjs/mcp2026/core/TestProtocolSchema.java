package com.ajaxjs.mcp2026.core;

import com.ajaxjs.mcp2026.core.schema.DefaultSchemaValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.ajaxjs.mcp2026.core.TestMcpCodec.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies official schema lookup and capability-sensitive protocol validation.
 */
class TestProtocolSchema {
    /**
     * Shared protocol-schema validator exercised by schema and concurrency cases.
     */
    private static final ProtocolSchema SCHEMA = new ProtocolSchema();

    private RpcRequest request(String method, Map<String, Object> params) {
        return RpcRequest.create(RequestId.of(1), method, params, RequestMetadata.create(map()));
    }

    @Test
    void validatesAllCoreRequestMethodsAndTheirRequiredParameters() {
        Map<String, Object> methods = map("server/discover", map(), "tools/list", map(),
                "tools/call", map("name", "tool"), "resources/list", map(), "resources/templates/list", map(),
                "resources/read", map("uri", "file:///a"), "prompts/list", map(), "prompts/get", map("name", "prompt"),
                "subscriptions/listen", map("notifications", map()), "completion/complete",
                map("ref", map("type", "ref/prompt", "name", "p"), "argument", map("name", "a", "value", "v")));
        methods.forEach((method, params) -> assertDoesNotThrow(() -> SCHEMA.requireRequest(request(method,
                WireValues.object(params, "params"))), method));
        McpProtocolException invalid = assertThrows(McpProtocolException.class,
                () -> SCHEMA.requireRequest(request("tools/call", map())));
        assertEquals(ErrorCodes.INVALID_PARAMS, invalid.getError().getCode());
        assertEquals(ErrorCodes.METHOD_NOT_FOUND, assertThrows(McpProtocolException.class,
                () -> SCHEMA.requireRequest(request("unknown", map()))).getError().getCode());
    }

    @Test
    void validatesContentAndToolDescriptors() {
        assertTrue(SCHEMA.validate("ContentBlock", map("type", "text", "text", "hello")).isEmpty());
        assertFalse(SCHEMA.validate("ContentBlock", map("type", "image", "data", "a")).isEmpty());
        assertFalse(SCHEMA.validate("Tool", map("name", "t", "inputSchema", map("type", "array"))).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.validate("unknown", map()));
    }

    @Test
    void validatesResultsAndCacheRequirements() {
        RpcRequest tools = request("tools/list", map());
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireResult(tools,
                new RpcSuccess(tools.getId(), McpResult.complete(map("tools", Collections.emptyList())))));
        SCHEMA.requireResult(tools, new RpcSuccess(tools.getId(), McpResult.complete(map("tools", Collections.emptyList(),
                "ttlMs", 0, "cacheScope", "private"))));
        RpcRequest discover = request("server/discover", map());
        SCHEMA.requireResult(discover, new RpcSuccess(discover.getId(), Discovery.result(map(), "server", "1")));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireResult(discover,
                new RpcSuccess(discover.getId(), McpResult.inputRequired(null, "state"))));
    }

    @Test
    void notificationErrorsNeverBecomeResponses() {
        SCHEMA.requireNotification(new RpcNotification("notifications/cancelled", map("requestId", 7)));
        McpProtocolException failure = assertThrows(McpProtocolException.class,
                () -> SCHEMA.requireNotification(new RpcNotification("notifications/progress", map("progress", "wrong"))));
        assertNull(failure.toErrorResponse());
    }

    @Test
    void mrtrChecksInputShapeAndPerRequestCapabilities() {
        RpcRequest noCaps = request("tools/call", map("name", "tool"));
        McpResult result = McpResult.inputRequired(map("roots", map("method", "roots/list")), "opaque");
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireResult(noCaps, new RpcSuccess(noCaps.getId(), result)));
        RpcRequest caps = RpcRequest.create(noCaps.getId(), noCaps.getMethod(), map("name", "tool"),
                RequestMetadata.create(map("roots", map())));
        SCHEMA.requireResult(caps, new RpcSuccess(caps.getId(), result));
        assertFalse(SCHEMA.validate("InputResponses", map("roots", map("roots", "bad"))).isEmpty());
    }

    @Test
    void toolContractValidatesArgumentsAndStructuredOutput() {
        ToolDefinition tool = ToolDefinition.create("tool", map("type", "object", "properties", map("age", map("type", "integer")),
                "required", Arrays.asList("age"))).with("outputSchema", map("type", Arrays.asList("string", "null")));
        ToolContract contract = new ToolContract(tool, new DefaultSchemaValidator(), SCHEMA);
        RpcRequest call = request("tools/call", map("name", "tool", "arguments", map("age", 1)));
        contract.requireCall(call);
        assertThrows(McpProtocolException.class, () -> contract.requireCall(request("tools/call", map("name", "tool"))));
        contract.requireResult(call, new RpcSuccess(call.getId(), McpResult.complete(map("content", Collections.emptyList(), "structuredContent", null))));
        assertThrows(IllegalArgumentException.class, () -> contract.requireResult(call,
                new RpcSuccess(call.getId(), McpResult.complete(map("content", Collections.emptyList(), "structuredContent", 1)))));
    }

    @Test
    void retryMatchesInputTypeAndPreservesState() {
        RpcRequest original = RpcRequest.create(RequestId.of(1), "tools/call", map("name", "tool"),
                RequestMetadata.create(map("roots", map())));
        RpcSuccess response = new RpcSuccess(original.getId(), McpResult.inputRequired(
                map("r", map("method", "roots/list")), "opaque"));
        RpcRequest good = MultiRoundTrip.retry(original, response, RequestId.of(2), map("r", map("roots", Collections.emptyList())));
        SCHEMA.requireRetry(original, response, good);
        RpcRequest wrongType = MultiRoundTrip.retry(original, response, RequestId.of(2), map("r", map("action", "cancel")));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireRetry(original, response, wrongType));
        Map<String, Object> changed = new java.util.LinkedHashMap<>(good.getParams());
        changed.put("requestState", "tampered");
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireRetry(original, response,
                new RpcRequest(good.getId(), good.getMethod(), changed)));
    }

    @Test
    void streamNotificationsMustMatchAcceptedTypeAndSubscription() {
        RpcRequest listen = Subscriptions.listen(RequestId.of(1), RequestMetadata.create(map()), map("toolsListChanged", true));
        RpcNotification ack = Subscriptions.acknowledge(listen, map("toolsListChanged", true));
        Map<String, Object> params = map("_meta", map(Subscriptions.SUBSCRIPTION_ID, 1));
        SCHEMA.requireSubscriptionNotification(listen, ack, new RpcNotification("notifications/tools/list_changed", params));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireSubscriptionNotification(listen, ack,
                new RpcNotification("notifications/prompts/list_changed", params)));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireSubscriptionNotification(listen, ack,
                new RpcNotification("notifications/tools/list_changed", map("_meta", map(Subscriptions.SUBSCRIPTION_ID, "1")))));
    }

    @Test
    void requestNotificationsRespectProgressTokenLogLevelAndDirection() {
        RpcRequest call = RpcRequest.create(RequestId.of(1), "tools/call", map("name", "tool"),
                RequestMetadata.create(map()).with("progressToken", "p").with("io.modelcontextprotocol/logLevel", "warning"));
        SCHEMA.requireRequestNotification(call, new RpcNotification("notifications/progress", map("progressToken", "p", "progress", 1)));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireRequestNotification(call,
                new RpcNotification("notifications/progress", map("progressToken", "other", "progress", 1))));
        SCHEMA.requireRequestNotification(call, new RpcNotification("notifications/message", map("level", "error", "data", "failed")));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireRequestNotification(call,
                new RpcNotification("notifications/message", map("level", "debug", "data", "hidden"))));
        assertThrows(McpProtocolException.class, () -> SCHEMA.requireClientNotification(
                new RpcNotification("notifications/progress", map("progressToken", "p", "progress", 1))));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireRequestNotification(call,
                new RpcNotification("notifications/cancelled", map("requestId", 1))));
    }

    @Test
    void reservedOutgoingErrorsAndFractionalCacheTtlAreRejected() {
        for (int code : new int[]{-32002, -32042, -32099})
            assertThrows(IllegalArgumentException.class, () -> ProtocolValidator.requireOutgoingError(new RpcError(RequestId.of(1), code, "error")));
        assertDoesNotThrow(() -> ProtocolValidator.requireOutgoingError(new RpcError(RequestId.of(1), -32022, "unsupported")));
        Map<String, Object> fields = new java.util.LinkedHashMap<>(Discovery.result(map(), "server", "1").toMap());
        fields.put("ttlMs", 0.5);
        assertThrows(IllegalArgumentException.class, () -> Discovery.validate(McpResult.from(fields)));
        assertThrows(IllegalArgumentException.class, () -> new McpCodec().encode(new RpcError(RequestId.of(1), -32042, "old")));
    }

    @Test
    void independentConcurrentValidationHasNoSharedRequestState() throws Exception {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            java.util.List<java.util.concurrent.Callable<Boolean>> jobs = new java.util.ArrayList<>();
            for (int i = 0; i < 24; i++) {
                final int id = i;
                jobs.add(() -> {
                    RpcRequest request = RpcRequest.create(RequestId.of(id), "tools/call", map("name", "tool"), RequestMetadata.create(map()));
                    SCHEMA.requireRequest(request);
                    SCHEMA.requireResult(request, new RpcSuccess(request.getId(), ToolResults.text("ok")));
                    return true;
                });
            }
            for (java.util.concurrent.Future<Boolean> result : executor.invokeAll(jobs, 10, java.util.concurrent.TimeUnit.SECONDS))
                assertTrue(result.get(1, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    @Test
    void validatesRemainingMethodResultsIncludingPaginationAndCompletionLimit() {
        String[][] lists = {{"resources/list", "resources"}, {"resources/templates/list", "resourceTemplates"},
                {"prompts/list", "prompts"}, {"resources/read", "contents"}};
        for (String[] pair : lists) {
            RpcRequest request = request(pair[0], "resources/read".equals(pair[0]) ? map("uri", "file:///a") : map());
            McpResult result = McpResult.complete(map(pair[1], Collections.emptyList(), "ttlMs", 10, "cacheScope", "private"));
            SCHEMA.requireResult(request, new RpcSuccess(request.getId(), result));
            assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireResult(request,
                    new RpcSuccess(request.getId(), McpResult.empty())));
        }
        RpcRequest prompt = request("prompts/get", map("name", "p"));
        SCHEMA.requireResult(prompt, new RpcSuccess(prompt.getId(), McpResult.complete(map("messages", Collections.emptyList()))));
        RpcRequest completion = request("completion/complete", map("ref", map("type", "ref/prompt", "name", "p"),
                "argument", map("name", "q", "value", "a")));
        SCHEMA.requireResult(completion, new RpcSuccess(completion.getId(), McpResult.complete(map("completion", map("values", Arrays.asList("a"))))));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireResult(completion, new RpcSuccess(completion.getId(),
                McpResult.complete(map("completion", map("values", Collections.nCopies(101, "a")))))));
    }

    @Test
    void elicitationChecksFormDataAgainstItsRequestedSchema() {
        RpcRequest original = RpcRequest.create(RequestId.of(1), "tools/call", map("name", "t"),
                RequestMetadata.create(map("elicitation", map("form", map()))));
        Map<String, Object> form = map("type", "object", "properties", map("age", map("type", "integer", "minimum", 18)),
                "required", Arrays.asList("age"));
        RpcSuccess input = new RpcSuccess(original.getId(), McpResult.inputRequired(map("q", map("method", "elicitation/create",
                "params", map("message", "Age?", "requestedSchema", form))), "state"));
        RpcRequest valid = MultiRoundTrip.retry(original, input, RequestId.of(2), map("q", map("action", "accept", "content", map("age", 20))));
        SCHEMA.requireRetry(original, input, valid);
        RpcRequest invalid = MultiRoundTrip.retry(original, input, RequestId.of(2), map("q", map("action", "accept", "content", map("age", 1))));
        assertThrows(IllegalArgumentException.class, () -> SCHEMA.requireRetry(original, input, invalid));
        RpcRequest declined = MultiRoundTrip.retry(original, input, RequestId.of(2), map("q", map("action", "decline")));
        SCHEMA.requireRetry(original, input, declined);
    }
}
