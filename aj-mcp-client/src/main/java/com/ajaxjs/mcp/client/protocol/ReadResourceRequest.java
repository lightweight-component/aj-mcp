package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ReadResourceRequest extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.RESOURCES_READ;

    @JsonInclude
    private Map<String, Object> params;

    public ReadResourceRequest(Long id, String uri) {
        super(id);
        this.params = new HashMap<>();
        Objects.requireNonNull(uri);
        this.params.put("uri", uri);
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
