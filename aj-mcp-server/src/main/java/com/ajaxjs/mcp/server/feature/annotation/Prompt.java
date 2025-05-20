package com.ajaxjs.mcp.server.feature.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a business method of a bean as an exposed prompt template.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Prompt {
    String value() default "";

    /**
     * An optional description.
     *
     * @return description
     */
    String description() default "";
}
