package com.foo.streamable;

import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

/** Sample-only bridge for annotation methods; scoped to the synchronous POST thread. */
public final class RequestContext {
    private final ThreadLocal<String> session = new ThreadLocal<>();
    private final ThreadLocal<Object> progressToken = new ThreadLocal<>();

    public void bind(String sessionId, String body) {
        session.set(sessionId);

        try {
            JsonNode token = JsonUtils.json2Node(body).path("params").path("_meta").path("progressToken");
            if (token.isTextual())
                progressToken.set(token.textValue());
            else if (token.isNumber())
                progressToken.set(token.numberValue());
        } catch (RuntimeException ignored) {
            // Protocol validation and error responses belong to the SDK, not this adapter.
        }
    }

    public String sessionId() {
        return session.get();
    }

    public Object progressToken() {
        return progressToken.get();
    }

    public void clear() {
        session.remove(); progressToken.remove();
    }
}
