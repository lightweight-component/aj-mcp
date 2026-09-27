package com.ajaxjs.mcp2026.core;

import lombok.experimental.UtilityClass;

import java.util.*;

/**
 * Factories for completed tool calls. Business errors remain successful RPC
 * envelopes with isError=true; structured content may be any JSON value.
 */
@UtilityClass
public class ToolResults {

    /**
     * Creates a successful complete result with one text content item.
     *
     * @param text text delivered to the client
     * @return complete non-error result
     */
    public static McpResult text(String text) {
        return text(text, false);
    }

    /**
     * Creates a complete tool result whose business-level error flag is set.
     *
     * @param message text delivered to the client
     * @return complete result with {@code isError=true}
     */
    public static McpResult error(String message) {
        return text(message, true);
    }

    /**
     * Builds the common complete result containing one text content item.
     *
     * @param text  text exposed to the MCP client
     * @param error whether the tool completed with a business-level error
     * @return a {@code complete} result
     */
    private static McpResult text(String text, boolean error) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "text");
        content.put("text", Objects.requireNonNull(text, "text"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", Collections.singletonList(content));
        result.put("isError", error);

        return McpResult.complete(result);
    }

    /**
     * Creates a complete result with content and arbitrary JSON structured content.
     *
     * @param content           MCP content items
     * @param structuredContent JSON value corresponding to the content
     * @param isError           whether the tool completed with a business-level error
     * @return complete result
     */
    public static McpResult structured(List<Map<String, Object>> content, Object structuredContent, boolean isError) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", Objects.requireNonNull(content, "content"));
        result.put("structuredContent", structuredContent);
        result.put("isError", isError);

        return McpResult.complete(result);
    }
}
