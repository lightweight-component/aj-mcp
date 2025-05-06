package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

@Deprecated
public class ListResourceTemplatesRequest extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.RESOURCES_TEMPLATES_LIST;

    public ListResourceTemplatesRequest(Long id) {
        super(id);
    }
}
