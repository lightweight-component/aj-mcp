package com.ajaxjs.mcp.protocol.tools;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Describes the JSON Schema accepted as the argument object of an MCP tool.
 *
 * <p>The regular fields model commonly used object-schema keywords. Unknown or
 * extension keywords are retained as {@link com.fasterxml.jackson.databind.JsonNode}
 * instances so that nested schemas and constraints can be deserialized and serialized
 * without losing their original JSON representation. Clients can use this schema to
 * validate arguments before sending a {@code tools/call} request.</p>
 */
@Data
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public class JsonSchema {
    // Keep extension keywords as trees so nested schemas and constraints round-trip.
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter(lombok.AccessLevel.NONE)
    private final java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> keywords =
            new java.util.LinkedHashMap<>();

    /**
     * Returns JSON Schema keywords that are not represented by dedicated fields.
     * The returned map is the live extension-keyword store used during serialization.
     *
     * @return extension keyword names and their JSON values
     */
    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> getKeywords() {
        return keywords;
    }

    /**
     * Records a JSON Schema keyword that is not represented by a dedicated property.
     * This method is used by Jackson while deserializing extension and nested keywords.
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
     * Sets a scalar JSON Schema {@code type} value and clears any previously retained
     * array or object form of that keyword.
     *
     * @param type simple schema type such as {@code object} or {@code string}, or {@code null}
     */
    public void setType(String type) {
        this.type = type;
        this.typeSchema = null;
    }

    /**
     * Returns the scalar {@code type} value when available, otherwise its preserved
     * JSON representation for array or other complex forms.
     *
     * @return simple type string, complex JSON type value, or {@code null}
     */
    @com.fasterxml.jackson.annotation.JsonGetter("type")
    public Object getTypeValue() {
        return typeSchema == null ? type : typeSchema;
    }

    /**
     * Accepts either the scalar or complex JSON representation of the {@code type}
     * keyword while preserving complex values for round-trip serialization.
     *
     * @param value JSON Schema {@code type} value
     */
    @com.fasterxml.jackson.annotation.JsonSetter("type")
    public void setTypeValue(com.fasterxml.jackson.databind.JsonNode value) {
        type = value.isTextual() ? value.textValue() : null;
        typeSchema = value.isTextual() ? null : value;
    }

    /**
     * Holds the properties value.
     */
    Map<String, JsonSchemaProperty> properties;

    /**
     * Holds the required value.
     */
    List<String> required;

    /**
     * Holds the additional properties value.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    Boolean additionalProperties;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.fasterxml.jackson.databind.JsonNode additionalPropertiesSchema;

    /**
     * Sets the boolean form of {@code additionalProperties} and clears a previously
     * retained schema-valued form.
     *
     * @param value whether properties not listed in {@code properties} are allowed
     */
    public void setAdditionalProperties(Boolean value) {
        additionalProperties = value;
        additionalPropertiesSchema = null;
    }

    /**
     * Returns the boolean {@code additionalProperties} setting or its preserved JSON
     * Schema form when restrictions are expressed as a nested schema.
     *
     * @return boolean setting, nested schema value, or {@code null}
     */
    @com.fasterxml.jackson.annotation.JsonGetter("additionalProperties")
    public Object getAdditionalPropertiesValue() {
        return additionalPropertiesSchema == null ? additionalProperties : additionalPropertiesSchema;
    }

    /**
     * Accepts either the boolean or schema-valued representation of
     * {@code additionalProperties}.
     *
     * @param value JSON value of the {@code additionalProperties} keyword
     */
    @com.fasterxml.jackson.annotation.JsonSetter("additionalProperties")
    public void setAdditionalPropertiesValue(com.fasterxml.jackson.databind.JsonNode value) {
        additionalProperties = value.isBoolean() ? value.booleanValue() : null;
        additionalPropertiesSchema = value.isBoolean() ? null : value;
    }
}
