package com.ajaxjs.mcp.server.feature.model;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * Base runtime binding for an annotated MCP feature method.
 * <p>
 * Subclasses add protocol metadata for tools, prompts, resources, templates, or completions,
 * while this base class keeps the Java object instance and reflective method used for invocation.
 */
@Data
public abstract class ServerStoreBase {
    /**
     * Service object that owns the annotated method.
     */
    Object instance;

    /**
     * Reflective method invoked when the matching MCP request is dispatched.
     */
    Method method;
}
