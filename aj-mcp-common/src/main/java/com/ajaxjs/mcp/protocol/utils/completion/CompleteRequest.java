package com.ajaxjs.mcp.protocol.utils.completion;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Collections;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.COMPLETION_COMPLETE;

/**
 * <a href="https://modelcontextprotocol.io/specification/2024-11-05/server/utilities/completion">...</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompleteRequest extends McpRequest {
    /**
     * Holds the method value.
     */
    String method = COMPLETION_COMPLETE;

    /**
     * Holds the params value.
     */
    Params params;

    /**
     * Represents params.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Params {
        /**
         * Holds the ref value.
         */
        Ref ref;

        /**
         * Holds the argument value.
         */
        Argument argument;

        /**
         * Previously resolved variables, introduced in MCP 2025-06-18.
         */
        @JsonIgnore
        Map<String, String> context;

        /** @return the MCP context.arguments object while keeping the Java Map API compatible. */
        @JsonProperty("context")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Map<String, Map<String, String>> getWireContext() {
            return context == null ? null : Collections.singletonMap("arguments", context);
        }

        /** @param value the MCP context object containing previously resolved arguments */
        @JsonProperty("context")
        public void setWireContext(Map<String, Map<String, String>> value) {
            context = value == null ? null : value.get("arguments");
        }

        /**
         * Creates a new params.
         *
         * @param ref      the ref value.
         * @param argument the argument value.
         */
        public Params(Ref ref, Argument argument) {
            this.ref = ref;
            this.argument = argument;
        }
    }

    /**
     * Reference Types
     * The protocol supports two types of completion references: ref/prompt|ref/resource
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ref {
        /**
         * The protocol supports two types of completion references: ref/prompt | ref/resource
         */
        String type;

        /**
         * References a prompt by name
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String name;

        /**
         * References a resource URI
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String uri;
    }

    /**
     * Represents argument.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Argument {
        /**
         * Holds the name value.
         */
        String name;
        /**
         * Holds the value value.
         */
        String value;
    }
}
