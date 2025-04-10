package com.ajaxjs.mcp.client.protocol.ping;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

public class PingResponse extends ClientMessage {
    // has to be an empty object
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final Map<String, Object> result = new HashMap<>();

    public PingResponse(Long id) {
        super(id);
    }
}
