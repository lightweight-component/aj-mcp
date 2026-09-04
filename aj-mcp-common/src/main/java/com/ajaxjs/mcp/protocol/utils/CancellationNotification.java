package com.ajaxjs.mcp.protocol.utils;

import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Cancellation Flow
 * When a party wants to cancel an in-progress request, it sends a notifications/cancelled notification containing:
 * The ID of the request to cancel;
 * An optional reason string that can be logged or displayed.
 * <a href="https://modelcontextprotocol.io/specification/2025-03-26/basic/utilities/cancellation">...</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CancellationNotification extends McpRequest {
    /**
     * Holds the method value.
     */
    private String method = McpConstant.Methods.NOTIFICATION_CANCELLED;

    /**
     * Holds the params value.
     */
    private Params params;

    /**
     * Represents params.
     */
    @Data
    public static class Params {
        /**
         * Holds the request id value.
         */
        private Object requestId;

        /**
         * Holds the reason value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String reason;
    }

    /**
     * Creates a new cancellation notification.
     *
     * @param requestId the request id value.
     */
    public CancellationNotification(Object requestId) {
        this.params = new Params();
        this.params.setRequestId(requestId);
    }

    /**
     * Creates a new cancellation notification.
     *
     * @param requestId the request id value.
     * @param message   the message value.
     */
    public CancellationNotification(Object requestId, String message) {
        this(requestId);
        this.params.setReason(message);
    }
}
