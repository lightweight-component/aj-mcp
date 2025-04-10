package com.ajaxjs.mcp.client.tool;

import com.ajaxjs.mcp.json.JsonObjectSchema;
import lombok.Data;

/**
 * Describes a tool that language model can execute.
 * <p>
 * Can be generated automatically from methods annotated with Tool using ToolSpecifications helper.
 */
@Data
public class ToolSpecification {
    private String name;

    private String description;

    private JsonObjectSchema parameters;
}
