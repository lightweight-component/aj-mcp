package com.ajaxjs.mcp.protocol.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Result returned by the client for sampling/createMessage.
 */
@Data
public class SamplingCreateMessageResult {
    private String role;
    private JsonNode content;
    private String model;
    private String stopReason;
}
