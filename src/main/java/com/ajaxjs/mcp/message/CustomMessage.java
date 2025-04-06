package com.ajaxjs.mcp.message;


import com.ajaxjs.mcp.McpUtils;

import java.util.Map;
import java.util.Objects;

import static com.ajaxjs.mcp.message.ChatMessageType.CUSTOM;

/**
 * Represents a custom message.
 * Can be used only with {@link ChatLanguageModel} implementations that support this type of message.
 */
public class CustomMessage implements ChatMessage {

    private final Map<String, Object> attributes;

    /**
     * Creates a new custom message.
     *
     * @param attributes the message attributes.
     */
    public CustomMessage(Map<String, Object> attributes) {
        this.attributes = McpUtils.copyIfNotNull(attributes);
    }

    /**
     * Returns the message attributes.
     *
     * @return the message attributes.
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    @Override
    public ChatMessageType type() {
        return CUSTOM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomMessage that = (CustomMessage) o;
        return Objects.equals(this.attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    @Override
    public String toString() {
        return "CustomMessage {" + " attributes = " + attributes + " }";
    }

    /**
     * Creates a new custom message.
     *
     * @param attributes the message attributes.
     * @return the custom message.
     */
    public static CustomMessage from(Map<String, Object> attributes) {
        return new CustomMessage(attributes);
    }

    /**
     * Creates a new custom message.
     *
     * @param attributes the message attributes.
     * @return the custom message.
     */
    public static CustomMessage customMessage(Map<String, Object> attributes) {
        return from(attributes);
    }
}
