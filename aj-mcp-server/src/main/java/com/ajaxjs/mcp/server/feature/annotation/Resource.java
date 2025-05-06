package com.ajaxjs.mcp.server.feature.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;


import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a business method of a CDI bean as an exposed resource.
 * <p>
 * The result of a "resource read" operation is always represented as a ResourceResponse. However, the annotated method
 * can also return other types that are converted according to the following rules.
 *
 * <p>
 * There is a default resource contents encoder registered; it encodes the returned value as JSON.
 *
 * @see ResourceTemplate
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Resource {
    /**
     * "A human-readable name for this resource."
     */
    String value() default "";

    /**
     * "A description of what this resource represents."
     */
    String description() default "";

    /**
     * "The URI of this resource."
     */
    String uri();

    /**
     * "The MIME type of this resource, if known."
     */
    String mimeType() default "";

}
