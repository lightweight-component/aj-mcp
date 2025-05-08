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
    private Long id;

    private String method;

    private JsonNode jsonNode;
}
