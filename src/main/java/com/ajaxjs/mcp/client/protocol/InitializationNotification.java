package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

public class InitializationNotification extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.NOTIFICATION_INITIALIZED;

    public InitializationNotification() {
        super(null);
    }
}
