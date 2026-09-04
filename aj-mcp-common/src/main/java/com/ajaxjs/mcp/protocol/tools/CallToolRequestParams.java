package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.utils.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Represents call tool request params.
 */
@Data
public class CallToolRequestParams {
    /**
     * Holds the name value.
     */
    private String name;

    /**
     * Holds the arguments value.
     */
    private Map<String, Object> arguments;

    /**
     * Holds the meta value.
     */
    @JsonProperty("_meta")
    private RequestMeta meta;
}