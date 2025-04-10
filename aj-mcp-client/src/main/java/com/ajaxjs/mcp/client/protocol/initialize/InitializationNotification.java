package com.ajaxjs.mcp.client.protocol.initialize;

import com.ajaxjs.mcp.client.protocol.ClientMessage;
import com.ajaxjs.mcp.client.protocol.ClientMethod;

public class InitializationNotification extends ClientMessage {
    public final ClientMethod method = ClientMethod.NOTIFICATION_INITIALIZED;

    public InitializationNotification() {
        super(null);
    }
}
