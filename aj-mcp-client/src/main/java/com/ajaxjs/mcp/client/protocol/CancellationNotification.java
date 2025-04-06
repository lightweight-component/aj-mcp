package com.ajaxjs.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

public class CancellationNotification extends ClientMessage {
    @JsonInclude
    public final ClientMethod method = ClientMethod.NOTIFICATION_CANCELLED;

    @JsonInclude
    private Map<String, Object> params;

    public CancellationNotification(Long requestId, String reason) {
        super(null);
        this.params = new HashMap<>();
        this.params.put("requestId", String.valueOf(requestId));

        if (reason != null)
            this.params.put("reason", reason);
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
