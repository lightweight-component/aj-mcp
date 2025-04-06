package com.ajaxjs.mcp.message;

/**
 * Represents a chat message.
 *
 * @see SystemMessage
 * @see UserMessage
 * @see AiMessage
 * @see ToolExecutionResultMessage
 */
public interface ChatMessage {

    /**
     * The type of the message.
     *
     * @return the type of the message
     */
    ChatMessageType type();
}
