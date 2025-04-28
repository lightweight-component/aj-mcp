package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Text content represents plain text messages.
 * This is the most common content type used for natural language interactions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptContentText extends PromptContent {
    String text;
}
