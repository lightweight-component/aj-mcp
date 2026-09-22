package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Runtime binding for one exposed MCP prompt.
 * <p>
 * The prompt metadata is advertised to clients, while the inherited method binding is used
 * to render prompt messages for a concrete argument set.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStorePrompt extends ServerStoreBase {
    /**
     * Prompt metadata returned by {@code prompts/list}.
     */
    PromptItem prompt;
}
