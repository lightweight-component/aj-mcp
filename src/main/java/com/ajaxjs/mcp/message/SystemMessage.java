package com.ajaxjs.mcp.message;

import com.ajaxjs.mcp.McpUtils;

import java.util.Objects;

import static com.ajaxjs.mcp.McpUtils.quoted;
import static com.ajaxjs.mcp.message.ChatMessageType.SYSTEM;

/**
 * Represents a system message, typically defined by a developer.
 * This type of message usually provides instructions regarding the AI's actions, such as its behavior or response style.
 */
public class SystemMessage implements ChatMessage {

    private final String text;

    /**
     * Creates a new system message.
     *
     * @param text the message text.
     */
    public SystemMessage(String text) {
        this.text = McpUtils.ensureNotBlank(text, "text");
    }

    /**
     * Returns the message text.
     *
     * @return the message text.
     */
    public String text() {
        return text;
    }

    @Override
    public ChatMessageType type() {
        return SYSTEM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemMessage that = (SystemMessage) o;
        return Objects.equals(this.text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "SystemMessage {" +
                " text = " + quoted(text) +
                " }";
    }

    /**
     * Creates a new system message.
     *
     * @param text the message text.
     * @return the system message.
     */
    public static SystemMessage from(String text) {
        return new SystemMessage(text);
    }

    /**
     * Creates a new system message.
     *
     * @param text the message text.
     * @return the system message.
     */
    public static SystemMessage systemMessage(String text) {
        return from(text);
    }
}