package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

public class ListPromptsRequest extends ClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.PROMPTS_LIST;

    public ListPromptsRequest(Long id) {
        super(id);
    }
}
