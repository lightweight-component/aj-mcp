package com.ajaxjs.mcp.protocol.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Represents params.
 */
@Data
public  class CancellationNotificationParams {
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