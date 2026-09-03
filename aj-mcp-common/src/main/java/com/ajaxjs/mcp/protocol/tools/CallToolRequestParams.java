package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.utils.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class CallToolRequestParams {
    private String name;

    private Map<String, Object> arguments;

    @JsonProperty("_meta")
    private RequestMeta meta;
}