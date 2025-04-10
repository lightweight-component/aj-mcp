package com.ajaxjs.mcp.client.protocol.resource;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.ClientMethod;
import com.fasterxml.jackson.annotation.JsonInclude;

public class ListResourceTemplatesRequest extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.RESOURCES_TEMPLATES_LIST;

    public ListResourceTemplatesRequest(Long id) {
        super(id);
    }
}
