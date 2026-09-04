package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;

import java.util.List;

/**
 * Represents get prompt result detail.
 */
@Data
public class GetPromptResultDetail {
    /**
     * Holds the description value.
     */
    String description;

    /**
     * Holds the messages value.
     */
    List<PromptMessage> messages;
}