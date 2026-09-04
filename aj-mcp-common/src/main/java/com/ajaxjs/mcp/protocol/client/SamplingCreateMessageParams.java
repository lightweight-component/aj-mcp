package com.ajaxjs.mcp.protocol.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * Parameters supplied by a server for sampling/createMessage.
 */
@Data
public class SamplingCreateMessageParams {
    /**
     * Holds the messages value.
     */
    private List<JsonNode> messages;

    /**
     * Holds the model preferences value.
     */
    private JsonNode modelPreferences;

    /**
     * Holds the system prompt value.
     */
    private String systemPrompt;

    /**
     * Holds the include context value.
     */
    private String includeContext;

    /**
     * Holds the temperature value.
     */
    private Double temperature;

    /**
     * Holds the max tokens value.
     */
    private int maxTokens;

    /**
     * Holds the stop sequences value.
     */
    private List<String> stopSequences;

    /**
     * Holds the metadata value.
     */
    private JsonNode metadata;
}
