package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStorePrompt extends ServerStoreBase {
    PromptItem prompt;
}
