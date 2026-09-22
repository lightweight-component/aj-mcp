package com.ajaxjs.mcp.server.feature.annotation;

import com.ajaxjs.mcp.protocol.McpConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation can be used to customize the name of a completed argument, i.e. the name of a parameter of a completion
 * method.
 * <p>
 * A completion method must consume exactly one {@link String} argument.
 *
 * @see CompletePrompt
 * @see CompleteResourceTemplate
 */
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CompleteArg {
    /**
     * Name of the prompt or resource-template argument for which this completion method
     * provides candidate values. When empty, the Java parameter name is used if available.
     *
     * @return the completed argument name, or an empty string to derive it from the parameter.
     */
    String name() default McpConstant.EMPTY_STR;

}
