package com.ajaxjs.mcp.client.protocol.ping;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.ClientMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

public class PingRequest extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.PING;

    public PingRequest(Long id) {
        super(id);
    }

}
