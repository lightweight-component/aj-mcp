package com.ajaxjs.mcp.client.json;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JsonEnumSchema implements JsonSchemaElement {
    private String description;
    private List<String> enumValues;

}
