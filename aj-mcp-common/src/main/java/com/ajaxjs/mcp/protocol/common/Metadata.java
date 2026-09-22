package com.ajaxjs.mcp.protocol.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Shared extension metadata, nested under _meta rather than flattened into the protocol object. */
public class Metadata {
    private Map<String, Object> meta;

    @JsonProperty("_meta")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Object> getMeta() { return meta; }

    @JsonProperty("_meta")
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
}
