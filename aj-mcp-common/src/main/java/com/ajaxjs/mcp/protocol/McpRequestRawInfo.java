package com.ajaxjs.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The raw information of McpRequest.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class McpRequestRawInfo {
    /**
     * Holds the id value.
     */
    private Object id;

    /**
     * Holds the method value.
     */
    private String method;

    /**
     * Holds the json node value.
     */
    private JsonNode jsonNode;
}
