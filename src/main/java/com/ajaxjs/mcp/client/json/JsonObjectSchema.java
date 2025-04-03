package com.ajaxjs.mcp.client.json;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Builder
@Data
public class JsonObjectSchema implements JsonSchemaElement {
    private String description;
    private Map<String, JsonSchemaElement> properties;
    private List<String> required;
    private Boolean additionalProperties;
    private Map<String, JsonSchemaElement> definitions;
}
