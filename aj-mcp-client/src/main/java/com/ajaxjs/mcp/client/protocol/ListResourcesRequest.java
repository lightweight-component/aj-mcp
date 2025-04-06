package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

public class ListResourcesRequest extends ClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.RESOURCES_LIST;

    public ListResourcesRequest(Long id) {
        super(id);
    }
}
