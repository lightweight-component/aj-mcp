package com.ajaxjs.mcp.protocol.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/** Parameters sent by a 2025-06-18 server in an {@code elicitation/create} request. */
@Data
public class ElicitRequestParams {
    private String message;

    /** Restricted JSON Schema describing the form fields requested from the user. */
    private Map<String, Object> requestedSchema;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> _meta;
}
