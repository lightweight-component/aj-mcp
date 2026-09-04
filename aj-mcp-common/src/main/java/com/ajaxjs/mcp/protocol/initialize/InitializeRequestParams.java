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
    /**
     * Holds the protocol version value.
     */
    private String protocolVersion;

    /**
     * Holds the capabilities value.
     */
    private Capabilities capabilities;

    /**
     * Holds the client info value.
     */
    private ClientInfo clientInfo;

    /**
     * Represents capabilities.
     */
    @Data
    public static class Capabilities {
        /**
         * Holds the roots value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Roots roots;

        /**
         * Holds the sampling value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Sampling sampling;

        /**
         * Holds the elicitation value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Elicitation elicitation;

        /**
         * Holds the experimental value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Experimental experimental;

        /**
         * Represents roots.
         */
        @Data
        public static class Roots {
            /**
             * Holds the list changed value.
             */
            private boolean listChanged;
        }

        /**
         * Represents sampling.
         */
        public static class Sampling {
        }

        /**
         * Client can answer server-initiated elicitation/create requests.
         */
        public static class Elicitation {
        }

        /**
         * Describes support for non-standard experimental features
         */
        public static class Experimental {
        }
    }

    /**
     * Represents client info.
     */
    @Data
    public static class ClientInfo {
        /**
         * Holds the name value.
         */
        private String name;

        /**
         * Holds the version value.
         */
        private String version;
    }
}
