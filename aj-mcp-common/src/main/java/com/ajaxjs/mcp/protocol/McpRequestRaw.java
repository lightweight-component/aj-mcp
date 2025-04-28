package com.ajaxjs.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class McpRequestRaw {
    private Long id;

    private String method;

    private JsonNode jsonNode;

    public McpRequestRaw(){}
}
