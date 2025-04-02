package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

public class InitializeRequest extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.INITIALIZE;

    private InitializeParams params;

    public InitializeRequest(Long id) {
        super(id);
    }

    public InitializeParams getParams() {
        return params;
    }

    public void setParams(InitializeParams params) {
        this.params = params;
    }
}
