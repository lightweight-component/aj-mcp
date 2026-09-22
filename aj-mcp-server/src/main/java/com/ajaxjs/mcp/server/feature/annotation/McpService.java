package com.ajaxjs.mcp.server.feature.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a class as a container of MCP feature methods.
 * <p>
 * Package scanning looks for this annotation before registering methods annotated with
 * {@link Tool}, {@link Prompt}, {@link Resource}, {@link ResourceTemplate}, or completion
 * annotations. The annotation has no attributes because registration details are declared
 * on individual methods.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface McpService {
}
