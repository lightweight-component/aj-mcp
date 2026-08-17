package com.ajaxjs.mcp.protocol.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * Parameters supplied by a server for sampling/createMessage.
 */
@Data
public class SamplingCreateMessageParams {
    private List<JsonNode> messages;
    private JsonNode modelPreferences;
    private String systemPrompt;
    private String includeContext;
    private Double temperature;
    private int maxTokens;
    private List<String> stopSequences;
    private JsonNode metadata;
}
