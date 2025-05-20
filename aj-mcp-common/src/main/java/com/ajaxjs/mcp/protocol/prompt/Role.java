package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    USER, ASSISTANT;

    /**
     * To allow case-insensitive deserialization
     *
     * @param key Key
     * @return Role
     */
    @JsonCreator
    public static Role fromString(String key) {
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(key))
                return role;
        }

        return null;
    }
}
