package com.ajaxjs.mcp.message;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

import static com.ajaxjs.mcp.message.ChatMessageType.CUSTOM;

/**
 * Represents a custom message.
 * Can be used only with ChatLanguageModel implementations that support this type of message.
 */
@Data
@AllArgsConstructor
public class CustomMessage implements ChatMessage {
    private final Map<String, Object> attributes;

    @Override
    public ChatMessageType type() {
        return CUSTOM;
    }
}
