package com.ajaxjs.mcp.server.feature.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a parameter of a {@link Prompt} method.
 */
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PromptArg {
    String value() default "";

    String description() default "";

    /**
     * An argument is required by default. However, if the annotated type is {@link Optional} and no annotation value is set
     * explicitly then the argument is not required.
     *
     * @return true if the argument is required, false otherwise
     */
    boolean required() default true;
}
