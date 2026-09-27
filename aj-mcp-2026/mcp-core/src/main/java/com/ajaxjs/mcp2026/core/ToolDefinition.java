package com.ajaxjs.mcp2026.core;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool descriptor preserving the entire schema, annotations and extension data.
 * Only descriptor shape is checked here; use a SchemaValidator for JSON Schema.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolDefinition {
    /**
     * Immutable complete tool descriptor, including unknown extension fields.
     */
    private final Map<String, Object> fields;


    /**
     * Creates a tool descriptor with the required name and object input schema.
     *
     * @param name        tool name
     * @param inputSchema JSON Schema object whose type is {@code object}
     * @return validated immutable descriptor
     */
    public static ToolDefinition create(String name, Map<String, Object> inputSchema) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("name", name);
        fields.put("inputSchema", inputSchema);

        return from(fields);
    }

    /**
     * Validates and snapshots a complete tool descriptor, preserving extension fields.
     *
     * @param fields complete descriptor fields
     * @return validated immutable descriptor
     */
    public static ToolDefinition from(Map<String, Object> fields) {
        Map<String, Object> snapshot = WireValues.object(fields, "tool");
        WireValues.string(snapshot.get("name"), "tool.name");
        Map<String, Object> input = WireValues.object(snapshot.get("inputSchema"), "tool.inputSchema");

        if (!"object".equals(input.get("type")))
            throw new IllegalArgumentException("Tool inputSchema.type must be object");

        if (snapshot.containsKey("outputSchema"))
            WireValues.object(snapshot.get("outputSchema"), "tool.outputSchema");

        if (snapshot.containsKey("annotations"))
            WireValues.object(snapshot.get("annotations"), "tool.annotations");

        return new ToolDefinition(snapshot);
    }

    /**
     * Returns a validated descriptor with one field replaced or added.
     *
     * @param key   descriptor field name
     * @param value JSON value for the field
     * @return validated immutable descriptor copy
     */
    public ToolDefinition with(String key, Object value) {
        Map<String, Object> next = new LinkedHashMap<>(fields);
        next.put(key, value);

        return from(next);
    }

    /**
     * Returns the tool's required protocol name.
     *
     * @return tool name
     */
    public String getName() {
        return (String) fields.get("name");
    }

    /**
     * Returns the immutable complete tool descriptor.
     *
     * @return descriptor fields including schemas and extension data
     */
    public Map<String, Object> toMap() {
        return fields;
    }
}
