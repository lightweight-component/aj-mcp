package com.ajaxjs.mcp.server.model;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Composite key for a JSON-RPC request id scoped to one transport session.
 * <p>
 * JSON-RPC ids are not globally unique across clients, so cancellation and running-request
 * tracking must include the session id to avoid affecting another client that reused the same id.
 */
@RequiredArgsConstructor
public final class RequestKey {
    /**
     * Transport session that owns the request.
     */
    private final String sessionId;

    /**
     * JSON-RPC request id as supplied by the client.
     */
    private final Object requestId;

    /**
     * Tests whether this request key belongs to the supplied session.
     *
     * @param sessionId the session id to compare.
     * @return {@code true} when both session ids are equal.
     */
    public boolean belongsTo(String sessionId) {
        return this.sessionId.equals(sessionId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof RequestKey))
            return false;
        RequestKey that = (RequestKey) other;
        return sessionId.equals(that.sessionId) && requestId.equals(that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, requestId);
    }
}