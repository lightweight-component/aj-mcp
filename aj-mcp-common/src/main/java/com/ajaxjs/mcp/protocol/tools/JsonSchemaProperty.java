package com.ajaxjs.mcp.protocol.tools;

import lombok.Data;

//import java.util.Map;

/**
 * Describes one named property in an object JSON Schema, which corresponds to a
 * parameter accepted by an MCP tool.
 *
 * <p>The custom codec supports both the boolean-schema shorthand and object-schema
 * form. Extension keywords are stored as JSON trees so constraints that do not have
 * dedicated fields can be round-tripped without information loss.</p>
 */
@Data
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = JsonSchemaPropertyCodec.Reader.class)
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = JsonSchemaPropertyCodec.Writer.class)
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class JsonSchemaProperty {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Boolean booleanSchema;
    // Keep extension keywords as trees so nested schemas and constraints round-trip.
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter(lombok.AccessLevel.NONE)
    private final java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> keywords =
            new java.util.LinkedHashMap<>();

    /**
     * Returns property-level JSON Schema keywords not represented by dedicated fields.
     * The returned map is the live store used by the custom serializer.
     *
     * @return extension keyword names and their JSON values
     */
    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> getKeywords() {
        return keywords;
    }

    /**
     * Records a property-level extension keyword during JSON deserialization.
     *
     * @param name  JSON Schema keyword name
     * @param value complete JSON value associated with the keyword
     */
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void setKeyword(String name, com.fasterxml.jackson.databind.JsonNode value) {
        keywords.put(name, value);
    }

    /**
     * Holds the type value.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String type;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.fasterxml.jackson.databind.JsonNode typeSchema;

    /**
     * Sets a scalar JSON Schema {@code type} value and discards any previously
     * retained complex representation.
     *
     * @param type simple schema type such as {@code string}, or {@code null}
     */
    public void setType(String type) {
        this.type = type;
        this.typeSchema = null;
    }

    /**
     * Returns the scalar type when available, or the original JSON representation for
     * an array or other complex form.
     *
     * @return simple type string, complex JSON value, or {@code null}
     */
    @com.fasterxml.jackson.annotation.JsonGetter("type")
    public Object getTypeValue() {
        return typeSchema == null ? type : typeSchema;
    }

    /**
     * Accepts either scalar or complex JSON Schema {@code type} values and preserves
     * complex values for serialization.
     *
     * @param value JSON Schema {@code type} value
     */
    @com.fasterxml.jackson.annotation.JsonSetter("type")
    public void setTypeValue(com.fasterxml.jackson.databind.JsonNode value) {
        type = value.isTextual() ? value.textValue() : null;
        typeSchema = value.isTextual() ? null : value;
    }

    /**
     * Holds the description value.
     */
    private String description;

// for complexParameter
//    private Map<String, JsonSchemaProperty> properties;

}
