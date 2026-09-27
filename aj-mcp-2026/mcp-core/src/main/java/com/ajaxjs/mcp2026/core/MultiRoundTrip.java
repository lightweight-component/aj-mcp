package com.ajaxjs.mcp2026.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stateless MRTR wire operations; no execution, retries, or state decoding.
 */
public final class MultiRoundTrip {
    /**
     * Prevents instantiation of stateless MRTR wire helpers.
     */
    private MultiRoundTrip() {
    }

    /**
     * Whether this core protocol method supports input_required results.
     *
     * @param method MCP method name
     * @return {@code true} when a complete result may be replaced by {@code input_required}
     */
    public static boolean supports(String method) {
        return "tools/call".equals(method) || "resources/read".equals(method) || "prompts/get".equals(method);
    }

    /**
     * Validates common continuation fields, not method-specific input schemas.
     *
     * @param params retry request parameters to validate
     * @throws IllegalArgumentException if a state value or response object has an invalid wire shape
     */
    public static void validateParams(Map<String, Object> params) {
        if (params.containsKey("requestState"))
            WireValues.string(params.get("requestState"), "requestState");
        if (params.containsKey("inputResponses")) {
            Map<String, Object> responses = WireValues.object(params.get("inputResponses"), "inputResponses");
            for (Object response : responses.values())
                WireValues.object(response, "inputResponse");
        }
    }

    /**
     * Constructs a new independent request with unchanged business arguments and
     * metadata. Only the latest round's state and responses are carried forward.
     * The caller must enforce capability checks, deadlines and round limits.
     *
     * @param original       original request whose business parameters are retained
     * @param response       correlated {@code input_required} response for the previous round
     * @param nextId         new request identifier, distinct from {@code original}'s identifier
     * @param inputResponses responses supplied for all requested inputs, or {@code null} when none are requested
     * @return independent retry request carrying only the permitted continuation fields
     * @throws IllegalArgumentException if correlation, result type, method, or required inputs are invalid
     */
    public static RpcRequest retry(RpcRequest original, RpcSuccess response, RequestId nextId,
                                   Map<String, Object> inputResponses) {
        if (!supports(original.getMethod()))
            throw new IllegalArgumentException("Method does not support MRTR");
        if (!original.getId().equals(response.getId()))
            throw new IllegalArgumentException("Response does not match request");
        if (original.getId().equals(nextId))
            throw new IllegalArgumentException("MRTR retry requires a different request ID");
        McpResult result = response.getResult();
        if (!McpResult.INPUT_REQUIRED.equals(result.getResultType()))
            throw new IllegalArgumentException("MRTR retry requires input_required");
        Map<String, Object> responses = WireValues.object(
                inputResponses == null ? Collections.emptyMap() : inputResponses, "inputResponses");
        Map<String, Object> fields = result.toMap();
        Map<String, Object> requested = fields.containsKey("inputRequests")
                ? WireValues.object(fields.get("inputRequests"), "inputRequests") : Collections.emptyMap();
        // A client retry must fulfill this round; a server receiving partial input
        // may ask again, so this completeness rule is not applied in validateParams.
        if (!responses.keySet().containsAll(requested.keySet()))
            throw new IllegalArgumentException("Missing requested input responses");
        Map<String, Object> params = new LinkedHashMap<>(original.getParams());
        params.remove("requestState");
        params.remove("inputResponses");
        if (fields.containsKey("requestState"))
            params.put("requestState", fields.get("requestState"));
        if (!responses.isEmpty() || fields.containsKey("inputRequests"))
            params.put("inputResponses", responses);
        return new RpcRequest(nextId, original.getMethod(), params);
    }
}
