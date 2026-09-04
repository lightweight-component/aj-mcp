package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents role.
 */
public enum Role {
    /**
     * Represents a user-authored prompt message.
     */
    USER,
    /**
     * Represents an assistant-authored prompt message.
     */
    ASSISTANT;

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
