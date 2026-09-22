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

    /** MCP roles are lowercase on the wire, including annotations.audience. */
    @com.fasterxml.jackson.annotation.JsonValue
    public String wireValue() { return name().toLowerCase(java.util.Locale.ROOT); }

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
