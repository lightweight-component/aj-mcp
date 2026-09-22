package com.ajaxjs.mcp.protocol.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata shared by MCP requests that support progress reporting.
 *
 * <p>In addition to the standard {@code progressToken}, this type retains vendor
 * extension members. Extension members are exposed as top-level JSON properties so
 * MCP peers can exchange non-standard metadata without changing the core model.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestMeta {
    /**
     * Preserve vendor metadata alongside the standard progress token.
     */
    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter(lombok.AccessLevel.NONE)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private final java.util.Map<String, Object> extensions = new java.util.LinkedHashMap<>();

    /**
     * Returns vendor-defined metadata that is serialized as additional top-level
     * members of the request metadata object.
     *
     * @return live map of extension names to extension values
     */
    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    /**
     * Stores an unrecognized metadata member as a vendor extension. Jackson invokes
     * this method when deserializing metadata members other than known fields.
     *
     * @param name  extension member name
     * @param value deserialized extension member value
     */
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void setExtension(String name, Object value) {
        extensions.put(name, value);
    }

    /**
     * Holds the progress token value.
     */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Object progressToken;
}
