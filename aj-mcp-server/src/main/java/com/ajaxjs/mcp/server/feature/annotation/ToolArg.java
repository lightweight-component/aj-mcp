package com.ajaxjs.mcp.server.feature.annotation;

import com.ajaxjs.mcp.protocol.McpConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes one argument of a method annotated with {@link Tool}.
 * <p>
 * The scanner uses this metadata to build the tool {@code inputSchema} and to map incoming
 * JSON object properties to Java method parameters.
 */
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ToolArg {
    /**
     * Argument name expected in the JSON object passed to {@code tools/call}. When empty,
     * the Java parameter name is used if available.
     *
     * @return the protocol argument name, or an empty string to derive it from the parameter.
     */
    String value() default McpConstant.EMPTY_STR;

    /**
     * Optional argument description included in the generated JSON Schema.
     *
     * @return the argument description, or an empty string when unspecified.
     */
    String description() default McpConstant.EMPTY_STR;

    /**
     * An argument is required by default. However, if the annotated type is {@link Optional} and no annotation value is set
     * explicitly then the argument is not required.
     *
     * @return true if the argument is required, false otherwise
     */
    boolean required() default true;
}
