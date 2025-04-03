package com.ajaxjs.mcp.client.json;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonArraySchema implements JsonSchemaElement {
    private String description;
    private JsonSchemaElement items;
}
