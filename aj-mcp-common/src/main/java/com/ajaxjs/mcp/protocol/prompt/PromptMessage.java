package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Prompt Detail
 */
public class PromptMessage {
    /**
     * Either “user” or “assistant” to indicate the speaker
     */
    Role role;

    PromptContent content;
}
