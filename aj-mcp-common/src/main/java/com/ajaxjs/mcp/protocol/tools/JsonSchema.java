package com.ajaxjs.mcp.protocol.tools;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * A JSON Schema object that describes the expected structure of the arguments when calling this tool.
 * This allows clients to validate tool arguments before sending them to the server.
 */
@Data
public class JsonSchema {
    String type;

    Map<String, JsonSchemaProperty> properties;

    List<String> required;

    Boolean additionalProperties;
}