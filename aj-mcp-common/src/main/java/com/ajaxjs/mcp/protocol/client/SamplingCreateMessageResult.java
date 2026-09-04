package com.ajaxjs.mcp.protocol.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Result returned by the client for sampling/createMessage.
 */
@Data
public class SamplingCreateMessageResult {
    /**
     * Holds the role value.
     */
    private String role;

    /**
     * Holds the content value.
     */
    private JsonNode content;

    /**
     * Holds the model value.
     */
    private String model;

    /**
     * Holds the stop reason value.
     */
    private String stopReason;
}
