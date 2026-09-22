package com.ajaxjs.mcp.server.feature.annotation;

import com.ajaxjs.mcp.protocol.McpConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a business method of a CDI bean as an exposed tool.
 * <p>
 * A result of a "tool call" operation is always represented as a ToolResponse. However, the annotated method can also
 * return other types that are converted according to the following rules.
 * <ul>
 * <li>If it returns {@link String} then the response is {@code success} and contains a single TextContent.</li>
 * <li>If it returns an implementation of Content then the response is {@code success} and contains a single
 * content object.</li>
 * <li>If it returns a {@link List} of Content implementations or strings then the response is
 * {@code success} and contains a list of relevant content objects.</li>
 * <li>If it returns any other type {@code X} or {@code List<X>} then {@code X} is encoded using the ToolResponseEncoder
 * and ContentEncoder API and afterwards the rules above apply.</li>
 * <li>It may also return a Uni that wraps any of the type mentioned above.</li>
 * </ul>
 *
 * <p>
 * There is a default content encoder registered; it encodes the returned value as JSON.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Tool {
    /**
     * Protocol name of the tool. When left empty, the Java method name is used.
     *
     * @return the exposed tool name, or an empty string to derive it from the method.
     */
    String value() default McpConstant.EMPTY_STR;

    /**
     * Optional tool description included in {@code tools/list} responses.
     *
     * @return the tool description, or an empty string when unspecified.
     */
    String description() default McpConstant.EMPTY_STR;

    /**
     * Optional display name: annotations.title since 2025-03-26, also top-level title since 2025-06-18.
     *
     * @return the display name, or an empty string when unspecified.
     */
    String title() default McpConstant.EMPTY_STR;

    /**
     * Optional JSON Schema object encoded as JSON (2025-06-18).
     *
     * @return the output schema JSON, or an empty string when unspecified.
     */
    String outputSchema() default McpConstant.EMPTY_STR;

    /**
     * Behavioral hints introduced in 2025-03-26. They are advisory, not security controls.
     *
     * @return whether the tool is expected to avoid modifying its environment.
     */
    boolean readOnlyHint() default false;

    /**
     * Indicates whether invoking the tool may perform destructive updates such as deleting
     * data, modifying external state, or triggering irreversible side effects.
     *
     * @return {@code true} when clients should treat calls as potentially destructive.
     */
    boolean destructiveHint() default true;

    /**
     * Indicates whether repeating the same tool call is expected to have the same effect as
     * calling it once.
     *
     * @return {@code true} when duplicate calls are expected to be safe.
     */
    boolean idempotentHint() default false;

    /**
     * Indicates whether the tool may interact with external systems outside the local server
     * process, such as files, networks, databases, or third-party APIs.
     *
     * @return {@code true} when the tool can access an open world beyond the MCP server.
     */
    boolean openWorldHint() default true;

}
