package com.ajaxjs.mcp.protocol.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * A JSON Schema object that describes the expected structure of the arguments when calling this tool.
 * This allows clients to validate tool arguments before sending them to the server.
 */
@Data
public class JsonSchema {
    /**
     * Holds the type value.
     */
    String type;

    /**
     * Holds the properties value.
     */
    Map<String, JsonSchemaProperty> properties;

    /**
     * Holds the required value.
     */
    List<String> required;

    /**
     * Holds the additional properties value.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean additionalProperties;
}