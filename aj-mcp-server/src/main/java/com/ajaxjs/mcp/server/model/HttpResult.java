package com.ajaxjs.mcp.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

/**
 * Represents http result.
 */
@Data
@AllArgsConstructor
public class HttpResult {
    /**
     * Holds the status value.
     */
    private int status;

    /**
     * Holds the headers value.
     */
    private Map<String, String> headers;

    /**
     * Holds the content type value.
     */
    private String contentType;

    /**
     * Holds the body value.
     */
    private String body;

    /**
     * Executes the json operation.
     *
     * @param status the status value.
     * @param body   the body value.
     * @return the result of the json operation.
     */
    public static HttpResult json(int status, String body) {
        return new HttpResult(status, Collections.<String, String>emptyMap(), "application/json", body);
    }

    /**
     * Executes the text operation.
     *
     * @param status the status value.
     * @param body   the body value.
     * @return the result of the text operation.
     */
    public static HttpResult text(int status, String body) {
        return new HttpResult(status, Collections.<String, String>emptyMap(), "text/plain", body);
    }
}