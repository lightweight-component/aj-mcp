package com.ajaxjs.mcp.protocol.utils;

import com.ajaxjs.mcp.protocol.McpRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.PROGRESS_NOTIFICATION;

/**
 * Progress notification associated with the token supplied in request metadata.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProgressNotification extends McpRequest {
    private String method = PROGRESS_NOTIFICATION;
    private Params params;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Params {
        private Object progressToken;
        private double progress;
        private Double total;

        /**
         * Optional human-readable status added in MCP 2025-03-26.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String message;

        public Params(Object progressToken, double progress, Double total) {
            this.progressToken = progressToken;
            this.progress = progress;
            this.total = total;
        }
    }
}
