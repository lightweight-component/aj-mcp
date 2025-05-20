package com.ajaxjs.mcp.server.feature.annotation;

import com.ajaxjs.mcp.protocol.McpConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a business method of a CDI bean as an exposed resource template.
 * <p>
 * There is a default resource contents encoder registered; it encodes the returned value as JSON.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface ResourceTemplate {
    /**
     * The human-readable name for this resource template.
     *
     * @return The human-readable name for this resource template.
     */
    String name() default McpConstant.EMPTY_STR;

    /**
     * The description of what this resource template represents.
     *
     * @return The description of what this resource template represents.
     */
    String description() default McpConstant.EMPTY_STR;

    /**
     * The Level 1 URI template that can be used to construct resource URIs.
     * <p>
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6570#section-1.2">the RFC 6570</a> for syntax definition.
     *
     * @return The URI template.
     */
    String uriTemplate();

    /**
     * The MIME type of this resource template.
     *
     * @return The MIME type of this resource template.
     */
    String mimeType() default McpConstant.EMPTY_STR;
}
