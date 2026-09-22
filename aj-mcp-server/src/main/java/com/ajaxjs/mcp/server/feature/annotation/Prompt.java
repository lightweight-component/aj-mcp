package com.ajaxjs.mcp.server.feature.annotation;

import com.ajaxjs.mcp.protocol.McpConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a business method as an MCP prompt template.
 * <p>
 * The annotated method is registered during feature scanning and is invoked when the peer
 * requests the prompt by name. Method parameters annotated with {@link PromptArg} become
 * prompt arguments exposed in the protocol metadata.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Prompt {
    /**
     * Protocol name of the prompt. When left empty, the Java method name is used.
     *
     * @return the exposed prompt name, or an empty string to derive it from the method.
     */
    String value() default McpConstant.EMPTY_STR;

    /**
     * Optional description shown to clients when listing prompts.
     *
     * @return the prompt description, or an empty string when unspecified.
     */
    String description() default McpConstant.EMPTY_STR;

    /**
     * Optional display title for protocol revisions that support prompt annotations.
     *
     * @return the display title, or an empty string when unspecified.
     */
    String title() default McpConstant.EMPTY_STR;
}
