package com.ajaxjs.mcp.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * Framework-neutral HTTP response returned by the Streamable HTTP adapter.
 * <p>
 * Servlet, Spring, or other web layers copy these fields to their native response objects
 * without requiring the core transport to depend on a specific HTTP framework.
 */
@Data
@AllArgsConstructor
public class HttpResult {
    /**
     * HTTP status code to send.
     */
    private int status;

    /**
     * Additional response headers, excluding the content type.
     */
    private Map<String, String> headers;

    /**
     * Media type for the response body.
     */
    private String contentType;

    /**
     * Response body text, usually a JSON-RPC payload or a diagnostic message.
     */
    private String body;

    /**
     * Creates an {@code application/json} response.
     *
     * @param status HTTP status code.
     * @param body   serialized JSON body.
     * @return the response descriptor.
     */
    public static HttpResult json(int status, String body) {
        return new HttpResult(status, Collections.<String, String>emptyMap(), "application/json", body);
    }

    /**
     * Creates a {@code text/plain} response, typically for HTTP-level validation failures.
     *
     * @param status HTTP status code.
     * @param body   plain-text response body.
     * @return the response descriptor.
     */
    public static HttpResult text(int status, String body) {
        return new HttpResult(status, Collections.<String, String>emptyMap(), "text/plain", body);
    }
}