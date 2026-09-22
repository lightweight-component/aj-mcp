package com.ajaxjs.mcp.server.feature.annotation;

import com.ajaxjs.mcp.protocol.McpConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes one argument accepted by a method annotated with {@link Prompt}.
 * <p>
 * Prompt arguments are advertised to clients and are converted from the request payload
 * before invoking the Java method.
 */
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PromptArg {
    /**
     * Prompt argument name exposed through the MCP protocol. When empty, the Java parameter
     * name is used if it is available at runtime.
     *
     * @return the protocol argument name, or an empty string to derive it from the parameter.
     */
    String value() default McpConstant.EMPTY_STR;

    /**
     * Optional argument description included in prompt metadata.
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
