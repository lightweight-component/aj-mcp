package com.ajaxjs.mcp.protocol.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of a 2025-06-18 elicitation request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElicitResult {
    /**
     * One of: accept, decline, cancel.
     */
    private String action;

    /**
     * Holds the content value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> content;

    /**
     * Executes the accept operation.
     *
     * @param content the content value.
     * @return the result of the accept operation.
     */
    public static ElicitResult accept(Map<String, Object> content) {
        return new ElicitResult("accept", content);
    }

    /**
     * Executes the decline operation.
     *
     * @return the result of the decline operation.
     */
    public static ElicitResult decline() {
        return new ElicitResult("decline", null);
    }

    /**
     * Executes the cancel operation.
     *
     * @return the result of the cancel operation.
     */
    public static ElicitResult cancel() {
        return new ElicitResult("cancel", null);
    }
}
