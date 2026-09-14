package com.ajaxjs.mcp.server.model;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Represents request key.
 */
@RequiredArgsConstructor
public final class RequestKey {
    /**
     * Holds the session id value.
     */
    private final String sessionId;

    /**
     * Holds the request id value.
     */
    private final Object requestId;

    /**
     * Executes the belonging to operation.
     *
     * @param sessionId the session id value.
     * @return the result of the belonging to operation.
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