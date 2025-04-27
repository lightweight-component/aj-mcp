package com.ajaxjs.mcp.protocol.initialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Protocol version supported
 * Client capabilities
 * Client implementation information
 */
@Data
public class InitializeRequestParams {
    private String protocolVersion;

    private Capabilities capabilities;

    private ClientInfo clientInfo;

    @Data
    public static class Capabilities {
        private Roots roots;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Sampling sampling;

        private Experimental experimental;

        @Data
        public static class Roots {
            private boolean listChanged;
        }

        public static class Sampling {
        }

        /**
         * Describes support for non-standard experimental features
         */
        public static class Experimental {
        }
    }

    @Data
    public static class ClientInfo {
        private String name;

        private String version;
    }
}
