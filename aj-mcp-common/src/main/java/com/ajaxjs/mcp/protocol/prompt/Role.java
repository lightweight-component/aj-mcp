package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Identifies the authoring role of an MCP prompt message.
 *
 * <p>The MCP wire format uses lowercase role names. Jackson serialization and
 * deserialization are therefore handled by {@link #wireValue()} and
 * {@link #fromString(String)} rather than exposing enum names directly.</p>
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
     * Returns the lowercase role identifier required by MCP JSON payloads. The same
     * representation is also used by values such as {@code annotations.audience}.
     *
     * @return lowercase wire representation of this role
     */
    @com.fasterxml.jackson.annotation.JsonValue
    public String wireValue() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Resolves a wire role name without requiring a particular letter case.
     *
     * @param key role name supplied by a peer
     * @return matching role, or {@code null} when the value is not a supported role
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
