package com.ajaxjs.mcp.protocol.initialize;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Protocol version supported
 * Server capabilities
 * Server implementation information
 */
@Data
public class InitializeResponseResult {
    /**
     * Holds the protocol version value.
     */
    private String protocolVersion;

    /**
     * Holds the capabilities value.
     */
    private Capabilities capabilities;

    /**
     * Holds the server info value.
     */
    private ServerInfo serverInfo;

    /**
     * Represents capabilities.
     */
    @Data
    public static class Capabilities {
        /**
         * Holds the prompts value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Prompts prompts;

        /**
         * Holds the resources value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Resources resources;

        /**
         * Holds the tools value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Tools tools;

        /**
         * Holds the logging value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Logging logging;

        /**
         * Holds the completions value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Completions completions;

        /**
         * Holds the experimental value.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Experimental experimental;

        /**
         * Offers prompt templates
         */
        @Data
        public static class Prompts {
            /**
             * listChanged indicates whether the server will emit notifications when the list of available prompts changes.
             */
            private boolean listChanged;
        }

        /**
         * Provides readable resources
         */
        @Data
        public static class Resources {
            /**
             * Holds the list changed value.
             */
            private boolean listChanged;

            /**
             * Support for subscribing to individual items’ changes
             */
            private boolean subscribe;
        }

        /**
         * Exposes callable tools
         */
        @Data
        public static class Tools {
            /**
             * Holds the list changed value.
             */
            private boolean listChanged;
        }

        /**
         * Emits structured log messages
         */
        public static class Logging {
        }

        /**
         * Server supports completion/complete.
         */
        public static class Completions {
        }

        /**
         * Describes support for non-standard experimental features
         */
        public static class Experimental {
        }
    }

    /**
     * Represents server info.
     */
    @Data
    public static class ServerInfo {
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
