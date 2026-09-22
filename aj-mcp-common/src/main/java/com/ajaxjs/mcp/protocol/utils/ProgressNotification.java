package com.ajaxjs.mcp.protocol.utils;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.PROGRESS_NOTIFICATION;

/**
 * Progress notification associated with the token supplied in request metadata.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProgressNotification extends McpRequest {
    /**
     * Holds the method value.
     */
    private String method = PROGRESS_NOTIFICATION;

    /**
     * Holds the params value.
     */
    private ProgressNotificationParams params;
}
