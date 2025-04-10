package com.ajaxjs.mcp.json;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JsonAnyOfSchema implements JsonSchemaElement {
    private String description;

    private List<JsonSchemaElement> anyOf;
}
