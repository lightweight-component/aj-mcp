package com.ajaxjs.mcp.json;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonStringSchema implements JsonSchemaElement {
    private String description;
}
