package com.ajaxjs.mcp.server.feature.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Binds a Java method parameter to a variable declared by a {@link ResourceTemplate} URI template.
 * <p>
 * During resource reads, the server extracts template variables from the requested URI and
 * supplies them to the annotated method parameters.
 */
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ResourceTemplateArg {
    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Name of the URI-template variable to bind. The default asks the scanner to use the Java
     * parameter name unchanged.
     *
     * @return the URI-template variable name or {@link #ELEMENT_NAME} to derive it.
     */
    String name() default ELEMENT_NAME;
}
