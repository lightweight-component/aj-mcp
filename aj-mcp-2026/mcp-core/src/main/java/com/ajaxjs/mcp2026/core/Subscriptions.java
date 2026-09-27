package com.ajaxjs.mcp2026.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Subscription wire helpers only; stream ordering and lifecycle belong to transports.
 */
public final class Subscriptions {
    /**
     * MCP method name used to start a long-lived notification subscription.
     */
    public static final String LISTEN = "subscriptions/listen";
    /**
     * MCP notification method used to acknowledge accepted subscription filters.
     */
    public static final String ACKNOWLEDGED = "notifications/subscriptions/acknowledged";
    /**
     * Metadata key that correlates subscription notifications with the listen request ID.
     */
    public static final String SUBSCRIPTION_ID = "io.modelcontextprotocol/subscriptionId";
    /**
     * Core boolean filter names accepted in a subscription acknowledgement.
     */
    private static final String[] FLAGS = {"toolsListChanged", "promptsListChanged", "resourcesListChanged"};

    /**
     * Prevents instantiation of stateless subscription wire helpers.
     */
    private Subscriptions() {
    }

    /**
     * Validates known fields and returns a deep immutable snapshot, preserving extensions.
     *
     * @param value notification filter object from a listen request or acknowledgement
     * @return immutable validated filter snapshot
     * @throws IllegalArgumentException if a known filter has an invalid type or URI list shape
     */
    public static Map<String, Object> filter(Object value) {
        Map<String, Object> filter = WireValues.object(value, "notifications");
        for (String flag : FLAGS)
            if (filter.containsKey(flag) && !(filter.get(flag) instanceof Boolean))
                throw new IllegalArgumentException(flag + " must be boolean");
        if (filter.containsKey("resourceSubscriptions")) {
            Object uris = filter.get("resourceSubscriptions");
            if (!(uris instanceof List))
                throw new IllegalArgumentException("resourceSubscriptions must be an array");
            for (Object uri : (List<?>) uris)
                WireValues.string(uri, "resourceSubscriptions[]");
        }
        return filter;
    }

    /**
     * Opens a logical subscription; this helper does not open a transport.
     *
     * @param id            request identifier for the listen operation
     * @param metadata      per-request version and client capabilities
     * @param notifications requested notification filter
     * @return validated {@code subscriptions/listen} request
     */
    public static RpcRequest listen(RequestId id, RequestMetadata metadata, Map<String, Object> notifications) {
        return RpcRequest.create(id, LISTEN, Collections.singletonMap("notifications", filter(notifications)), metadata);
    }

    /**
     * Builds the first notification. Accepted core filters must be a subset of the request.
     *
     * @param request  originating listen request
     * @param accepted server-approved notification filter
     * @return correlated acknowledgement notification
     * @throws IllegalArgumentException if the request or accepted subset is invalid
     */
    public static RpcNotification acknowledge(RpcRequest request, Map<String, Object> accepted) {
        requireListen(request);
        Map<String, Object> requested = filter(request.getParams().get("notifications"));
        Map<String, Object> actual = filter(accepted);

        for (String flag : FLAGS)
            if (Boolean.TRUE.equals(actual.get(flag)) && !Boolean.TRUE.equals(requested.get(flag)))
                throw new IllegalArgumentException("Unrequested subscription: " + flag);

        if (actual.containsKey("resourceSubscriptions")) {
            List<?> uris = (List<?>) actual.get("resourceSubscriptions");
            List<?> allowed = requested.containsKey("resourceSubscriptions")
                    ? (List<?>) requested.get("resourceSubscriptions") : Collections.emptyList();

            if (!allowed.containsAll(uris))
                throw new IllegalArgumentException("Unrequested resource subscription");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("_meta", Collections.singletonMap(SUBSCRIPTION_ID, request.getId().getValue()));
        params.put("notifications", actual);
        return new RpcNotification(ACKNOWLEDGED, params);
    }

    /**
     * Reads correlation metadata without conflating numeric and string IDs.
     *
     * @param notification subscription acknowledgement or stream notification
     * @return exact typed subscription request identifier from notification metadata
     * @throws IllegalArgumentException if required, correlation metadata is absent or malformed
     */
    public static RequestId notificationId(RpcNotification notification) {
        Map<String, Object> params = WireValues.object(notification.getParams(), "params");
        Map<String, Object> meta = WireValues.object(params.get("_meta"), "params._meta");

        if (ACKNOWLEDGED.equals(notification.getMethod()))
            filter(params.get("notifications"));

        return RequestId.from(meta.get(SUBSCRIPTION_ID));
    }

    /**
     * Completion is sent at graceful closure, never as the opening acknowledgment.
     *
     * @param request originating listen request
     * @return correlated final success response with subscription metadata
     */
    public static RpcSuccess closed(RpcRequest request) {
        requireListen(request);

        return new RpcSuccess(request.getId(), McpResult.complete(Collections.singletonMap("_meta",
                Collections.singletonMap(SUBSCRIPTION_ID, request.getId().getValue()))));
    }

    /**
     * Checks a decoded completion against its originating listen request.
     *
     * @param request  originating listen request
     * @param response final success response to validate
     * @throws IllegalArgumentException if the response is not the valid correlated close result
     */
    public static void validateClosed(RpcRequest request, RpcSuccess response) {
        requireListen(request);
        Map<String, Object> meta = WireValues.object(response.getResult().toMap().get("_meta"), "result._meta");
        if (!McpResult.COMPLETE.equals(response.getResult().getResultType())
                || !request.getId().equals(response.getId())
                || !request.getId().equals(RequestId.from(meta.get(SUBSCRIPTION_ID))))
            throw new IllegalArgumentException("Invalid subscription completion correlation");
    }

    /**
     * Rejects a request that is not a subscription listen operation.
     *
     * @param request request expected to use {@link #LISTEN}
     * @throws IllegalArgumentException if the request method differs
     */
    private static void requireListen(RpcRequest request) {
        if (!LISTEN.equals(request.getMethod()))
            throw new IllegalArgumentException("Expected subscriptions/listen");
    }
}
