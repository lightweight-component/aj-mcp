package com.ajaxjs.mcp.client.protocol.tool;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.ClientMethod;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public class CallToolRequest extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.TOOLS_CALL;

    @JsonInclude
    private Map<String, Object> params = new HashMap<>();

    public CallToolRequest(Long id, String toolName, ObjectNode arguments) {
        super(id);
        this.params.put("name", toolName);
        this.params.put("arguments", arguments);
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
