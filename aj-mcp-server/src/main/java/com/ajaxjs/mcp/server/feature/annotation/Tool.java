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
     * Executes the value operation.
     *
     * @return the result of the value operation.
     */
    String value() default McpConstant.EMPTY_STR;

    /**
     * An optional description.
     *
     * @return description
     */
    String description() default McpConstant.EMPTY_STR;

    /**
     * Optional human-facing display name (2025-06-18).
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
     * Executes the destructive hint operation.
     *
     * @return the result of the destructive hint operation.
     */
    boolean destructiveHint() default true;

    /**
     * Executes the idempotent hint operation.
     *
     * @return the result of the idempotent hint operation.
     */
    boolean idempotentHint() default false;

    /**
     * Executes the open world hint operation.
     *
     * @return the result of the open world hint operation.
     */
    boolean openWorldHint() default true;

}
