package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents server store prompt.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStorePrompt extends ServerStoreBase {
    /**
     * Holds the prompt value.
     */
    PromptItem prompt;
}
