package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class ClientMessage {
    @JsonInclude
    public final String jsonrpc = "2.0";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;

    public ClientMessage(Long id) {
        this.id = id;
    }
}
