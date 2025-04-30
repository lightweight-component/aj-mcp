package com.ajaxjs.mcp.protocol;

/**
 * Constants
 */
public interface McpConstant {
    class Methods {
        public static final String INITIALIZE = "initialize";
        public static final String TOOLS_CALL = "tools/call";
        public static final String TOOLS_LIST = "tools/list";
        public static final String NOTIFICATION_CANCELLED = "notifications/cancelled";
        public static final String NOTIFICATION_INITIALIZED = "notifications/initialized";
        public static final String PING = "ping";
        public static final String RESOURCES_LIST = "resources/list";
        public static final String RESOURCES_READ = "resources/read";
        public static final String RESOURCES_TEMPLATES_LIST = "resources/templates/list";
        public static final String PROMPTS_LIST = "prompts/list";
        public static final String PROMPTS_GET = "prompts/get";
    }

    interface PromptContentType {
        String TEXT = "text";
        String IMAGE = "image";

        String AUDIO = "audio";

        String RESOURCE = "resource";
    }
}
