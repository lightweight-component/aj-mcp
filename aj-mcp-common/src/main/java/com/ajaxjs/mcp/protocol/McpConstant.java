package com.ajaxjs.mcp.protocol;

/**
 * Constants
 */
public interface McpConstant {
    /**
     * Represents methods.
     */
    class Methods {
        /**
         * Defines the initialize constant.
         */
        public static final String INITIALIZE = "initialize";

        /**
         * Defines the notification cancelled constant.
         */
        public static final String NOTIFICATION_CANCELLED = "notifications/cancelled";

        /**
         * Defines the notification initialized constant.
         */
        public static final String NOTIFICATION_INITIALIZED = "notifications/initialized";

        /**
         * Defines the ping constant.
         */
        public static final String PING = "ping";

        /**
         * Defines the resources list constant.
         */
        public static final String RESOURCES_LIST = "resources/list";

        /**
         * Defines the resources read constant.
         */
        public static final String RESOURCES_READ = "resources/read";

        /**
         * Defines the resources templates list constant.
         */
        public static final String RESOURCES_TEMPLATES_LIST = "resources/templates/list";

        /**
         * Defines the resource list changed notification constant.
         */
        public static final String RESOURCE_LIST_CHANGED_NOTIFICATION = "notifications/resources/list_changed";

        /**
         * Defines the resources subscribe request constant.
         */
        public static final String RESOURCES_SUBSCRIBE_REQUEST = "resources/subscribe";

        /**
         * Defines the resources unsubscribe request constant.
         */
        public static final String RESOURCES_UNSUBSCRIBE_REQUEST = "resources/unsubscribe";

        /**
         * Defines the resource update notification constant.
         */
        public static final String RESOURCE_UPDATE_NOTIFICATION = "notifications/resources/updated";

        /**
         * Defines the prompts list constant.
         */
        public static final String PROMPTS_LIST = "prompts/list";

        /**
         * Defines the prompts get constant.
         */
        public static final String PROMPTS_GET = "prompts/get";

        /**
         * Defines the prompts list changed notification constant.
         */
        public static final String PROMPTS_LIST_CHANGED_NOTIFICATION = "notifications/prompts/list_changed";

        /**
         * Defines the tools list constant.
         */
        public static final String TOOLS_LIST = "tools/list";

        /**
         * Defines the tools call constant.
         */
        public static final String TOOLS_CALL = "tools/call";

        /**
         * Defines the tools list changed notification constant.
         */
        public static final String TOOLS_LIST_CHANGED_NOTIFICATION = "notifications/tools/list_changed";

        /**
         * Defines the completion complete constant.
         */
        public static final String COMPLETION_COMPLETE = "completion/complete";

        /**
         * Defines the logging set level constant.
         */
        public static final String LOGGING_SET_LEVEL = "logging/setLevel";

        /**
         * Defines the logging message notification constant.
         */
        public static final String LOGGING_MESSAGE_NOTIFICATION = "notifications/message";

        /**
         * Defines the progress notification constant.
         */
        public static final String PROGRESS_NOTIFICATION = "notifications/progress";

        /**
         * Defines the roots list constant.
         */
        public static final String ROOTS_LIST = "roots/list";

        /**
         * Defines the roots list changed notification constant.
         */
        public static final String ROOTS_LIST_CHANGED_NOTIFICATION = "notifications/roots/list_changed";

        /**
         * Defines the sampling create message constant.
         */
        public static final String SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

        /**
         * Defines the elicitation create constant.
         */
        public static final String ELICITATION_CREATE = "elicitation/create";
    }

    /**
     * Represents content type.
     */
    interface ContentType {
        /**
         * Defines the text constant.
         */
        String TEXT = "text";

        /**
         * Defines the image constant.
         */
        String IMAGE = "image";

        /**
         * Defines the audio constant.
         */
        String AUDIO = "audio";

        /**
         * Defines the resource constant.
         */
        String RESOURCE = "resource";

        /**
         * Defines the resource link constant.
         */
        String RESOURCE_LINK = "resource_link";
    }

    /**
     * Defines the response result constant.
     */
    String RESPONSE_RESULT = "result";

    /**
     * Defines the id constant.
     */
    String ID = "id";

    /**
     * Defines the params constant.
     */
    String PARAMS = "params";

    /**
     * Defines the method constant.
     */
    String METHOD = "method";

    /**
     * Defines the empty str constant.
     */
    String EMPTY_STR = "";
}
