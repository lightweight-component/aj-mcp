package com.ajaxjs.mcp.protocol.prompt;

import com.ajaxjs.mcp.protocol.common.Content;
import lombok.Data;

/**
 * Prompt Detail
 */
@Data
public class PromptMessage {
    /**
     * Either “user” or “assistant” to indicate the speaker
     */
    private Role role;

    private Content content;
}
