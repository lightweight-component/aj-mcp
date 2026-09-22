package com.ajaxjs.mcp.server.feature.annotation;


import com.ajaxjs.mcp.protocol.McpConstant;

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
     * A human-readable name for this resource.
     *
     * @return A human-readable name for this resource.
     */
    String value() default McpConstant.EMPTY_STR;

    /**
     * A description of what this resource represents.
     *
     * @return A description of what this resource represents.
     */
    String description() default McpConstant.EMPTY_STR;

    /**
     * Stable URI that identifies this concrete resource in the MCP resource catalog.
     *
     * @return the resource URI sent to clients and later used for read requests.
     */
    String uri();

    /**
     * The MIME type of this resource, if known.
     *
     * @return The MIME type of this resource, if known.
     */
    String mimeType() default McpConstant.EMPTY_STR;

    /**
     * Optional display title for protocol revisions that support resource annotations.
     *
     * @return the display title, or an empty string when unspecified.
     */
    String title() default McpConstant.EMPTY_STR;

}
