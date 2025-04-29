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
    private String method = McpConstant.Methods.NOTIFICATION_CANCELLED;

    private Params params;

    @Data
    public static class Params {
        private String requestId;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String reason;
    }

    public CancellationNotification(String requestId) {
        this.params = new Params();
        this.params.setRequestId(requestId);
    }

    public CancellationNotification(String requestId, String message) {
        this(requestId);
        this.params.setReason(message);
    }
}
