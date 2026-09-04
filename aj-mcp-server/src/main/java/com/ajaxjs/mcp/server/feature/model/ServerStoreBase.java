package com.ajaxjs.mcp.server.feature.model;

import lombok.Data;

import java.lang.reflect.Method;

/**
 * Represents server store base.
 */
@Data
public abstract class ServerStoreBase {
    /**
     * Holds the instance value.
     */
    Object instance;

    /**
     * Holds the method value.
     */
    Method method;
}
