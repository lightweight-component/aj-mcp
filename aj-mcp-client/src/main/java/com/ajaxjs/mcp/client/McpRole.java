package com.ajaxjs.mcp.client;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum McpRole {
    ASSISTANT,
    USER;

    // to allow case-insensitive deserialization
    @JsonCreator
    public static McpRole fromString(String key) {
        for (McpRole role : McpRole.values()) {
            if (role.name().equalsIgnoreCase(key))
                return role;
        }

        return null;
    }
}
