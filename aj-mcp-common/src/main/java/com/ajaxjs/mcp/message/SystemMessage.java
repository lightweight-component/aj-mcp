package com.ajaxjs.mcp.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.ajaxjs.mcp.message.ChatMessageType.SYSTEM;

/**
 * Represents a system message, typically defined by a developer.
 * This type of message usually provides instructions regarding the AI's actions, such as its behavior or response style.
 */
@Data
@AllArgsConstructor
public class SystemMessage implements ChatMessage {
    private final String text;

    @Override
    public ChatMessageType type() {
        return SYSTEM;
    }

}