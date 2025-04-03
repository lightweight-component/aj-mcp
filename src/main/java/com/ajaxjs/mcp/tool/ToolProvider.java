package com.ajaxjs.mcp.tool;

@FunctionalInterface
public interface ToolProvider {
    ToolProviderResult provideTools(ToolProviderRequest var1);
}