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
    private String protocolVersion;

    private Capabilities capabilities;

    private ServerInfo serverInfo;

    @Data
    public static class Capabilities {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Prompts prompts;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Resources resources;

        private Tools tools;

        private Logging logging = new Logging();

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
            private boolean listChanged;
        }

        /**
         * Emits structured log messages
         */
        public static class Logging {
        }

        /**
         * Describes support for non-standard experimental features
         */
        public static class Experimental {
        }
    }

    @Data
    public static class ServerInfo {
        private String name;

        private String version;
    }
}
