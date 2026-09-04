package com.ajaxjs.mcp.protocol.utils;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    private Params params;

    /**
     * Represents params.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
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
        public Params(Object progressToken, double progress, Double total) {
            this.progressToken = progressToken;
            this.progress = progress;
            this.total = total;
        }
    }
}
