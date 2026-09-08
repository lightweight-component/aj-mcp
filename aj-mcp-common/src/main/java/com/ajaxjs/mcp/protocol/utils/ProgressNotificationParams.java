package com.ajaxjs.mcp.protocol.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * Represents params.
 */
@Data
@AllArgsConstructor
public class ProgressNotificationParams {
    /**
     * Holds the progress token value.
     */
    private Object progressToken;

    /**
     * Holds the progress value.
     */
    private double progress;

    /**
     * Holds the total value.
     */
    private Double total;

    /**
     * Optional human-readable status added in MCP 2025-03-26.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    /**
     * Creates a new params.
     *
     * @param progressToken the progress token value.
     * @param progress      the progress value.
     * @param total         the total value.
     */
    public ProgressNotificationParams(Object progressToken, double progress, Double total) {
        this.progressToken = progressToken;
        this.progress = progress;
        this.total = total;
    }
}