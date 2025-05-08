package com.ajaxjs.mcp.protocol;

/**
 * Constants
 */
public interface McpConstant {
    class Methods {
        public static final String INITIALIZE = "initialize";

        public static final String NOTIFICATION_CANCELLED = "notifications/cancelled";

        public static final String NOTIFICATION_INITIALIZED = "notifications/initialized";

        public static final String PING = "ping";

        public static final String RESOURCES_LIST = "resources/list";

        public static final String RESOURCES_READ = "resources/read";

        public static final String RESOURCES_TEMPLATES_LIST = "resources/templates/list";

        public static final String RESOURCE_LIST_CHANGED_NOTIFICATION = "notifications/resources/list_changed";

        public static final String RESOURCES_SUBSCRIBE_REQUEST = "resources/subscribe";

        public static final String RESOURCE_UPDATE_NOTIFICATION = "notifications/resources/updated";

        public static final String PROMPTS_LIST = "prompts/list";

        public static final String PROMPTS_GET = "prompts/get";

        public static final String PROMPTS_LIST_CHANGED_NOTIFICATION = "notifications/prompts/list_changed";

        public static final String TOOLS_LIST = "tools/list";

        public static final String TOOLS_CALL = "tools/call";

        public static final String TOOLS_LIST_CHANGED_NOTIFICATION = "notifications/tools/list_changed";
    }

    interface ContentType {
        String TEXT = "text";

        String IMAGE = "image";

        String AUDIO = "audio";

        String RESOURCE = "resource";
    }

    String RESPONSE_RESULT = "result";

    String PARAMS = "params";
}
