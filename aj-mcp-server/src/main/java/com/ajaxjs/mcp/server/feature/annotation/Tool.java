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
    String value() default McpConstant.EMPTY_STR;

    /**
     * An optional description.
     *
     * @return description
     */
    String description() default McpConstant.EMPTY_STR;

}
