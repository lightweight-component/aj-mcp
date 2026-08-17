package com.ajaxjs.mcp.protocol.utils.completion;

import com.ajaxjs.mcp.protocol.McpRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.COMPLETION_COMPLETE;

import java.util.Map;

/**
 * https://modelcontextprotocol.io/specification/2024-11-05/server/utilities/completion
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompleteRequest extends McpRequest {
    String method = COMPLETION_COMPLETE;

    Params params;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Params {
        Ref ref;
        Argument argument;

        /**
         * Previously resolved variables, introduced in MCP 2025-06-18.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, String> context;

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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Argument {
        String name;
        String value;
    }
}
