package com.ajaxjs.mcp.client.protocol.initialize;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.ClientMethod;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitializeRequest extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.INITIALIZE;

    private InitializeRequestParams params;

    public InitializeRequest(Long id) {
        super(id);
    }
}
