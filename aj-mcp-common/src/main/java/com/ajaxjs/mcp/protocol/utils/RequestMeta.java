package com.ajaxjs.mcp.protocol.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata shared by MCP requests that support progress reporting.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestMeta {
    /** Preserve vendor metadata alongside the standard progress token. */
    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter(lombok.AccessLevel.NONE)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private final java.util.Map<String, Object> extensions = new java.util.LinkedHashMap<>();

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<String, Object> getExtensions() { return extensions; }

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void setExtension(String name, Object value) { extensions.put(name, value); }
    /**
     * Holds the progress token value.
     */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Object progressToken;
}
