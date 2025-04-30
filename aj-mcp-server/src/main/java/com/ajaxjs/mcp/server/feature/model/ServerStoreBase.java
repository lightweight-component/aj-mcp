package com.ajaxjs.mcp.server.feature.model;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public abstract class ServerStoreBase {
    Object instance;

    Method method;
}
