package com.ajaxjs.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The raw information of McpRequest.
 */
@Data
@AllArgsConstructor
public class McpRequestRawInfo {
    private Long id;

    private String method;

    private JsonNode jsonNode;

    public McpRequestRawInfo() {
    }
}
