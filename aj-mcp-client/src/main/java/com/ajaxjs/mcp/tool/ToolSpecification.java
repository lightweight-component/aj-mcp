package com.ajaxjs.mcp.tool;

import com.ajaxjs.mcp.client.json.JsonObjectSchema;
import lombok.Data;

/**
 * Describes a tool that language model can execute.
 * <p>
 * Can be generated automatically from methods annotated with {@link Tool} using {@link ToolSpecifications} helper.
 */
@Data
public class ToolSpecification {
    private String name;

    private String description;

    private JsonObjectSchema parameters;
}
