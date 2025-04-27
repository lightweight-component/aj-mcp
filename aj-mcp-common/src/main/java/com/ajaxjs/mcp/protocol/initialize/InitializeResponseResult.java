package com.ajaxjs.mcp.protocol.initialize;

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
        private Prompts prompts;

        private Resources resources;

        private Tools tools;

        private Logging logging;

        private Experimental experimental;

        /**
         * Offers prompt templates
         */
        @Data
        public static class Prompts {
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
