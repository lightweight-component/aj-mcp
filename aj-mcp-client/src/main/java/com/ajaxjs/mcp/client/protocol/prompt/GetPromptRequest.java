package com.ajaxjs.mcp.client.protocol.prompt;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.ClientMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

public class GetPromptRequest extends ClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.PROMPTS_GET;

    @JsonInclude
    private Map<String, Object> params;

    public GetPromptRequest(Long id, String promptName, Map<String, Object> arguments) {
        super(id);
        this.params = new HashMap<>();
        this.params.put("name", promptName);
        this.params.put("arguments", arguments);
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
