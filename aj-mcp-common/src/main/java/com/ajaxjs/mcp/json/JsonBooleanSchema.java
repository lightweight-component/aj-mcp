package com.ajaxjs.mcp.json;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonBooleanSchema implements JsonSchemaElement {
    private String description;
}
