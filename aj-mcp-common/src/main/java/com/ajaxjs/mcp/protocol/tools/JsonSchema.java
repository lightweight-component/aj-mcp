package com.ajaxjs.mcp.protocol.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * A JSON Schema object that describes the expected structure of the arguments when calling this tool.
 * This allows clients to validate tool arguments before sending them to the server.
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

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> getKeywords() {
        return keywords;
    }

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

    public void setType(String type) {
        this.type = type;
        this.typeSchema = null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("type")
    public Object getTypeValue() {
        return typeSchema == null ? type : typeSchema;
    }

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

    public void setAdditionalProperties(Boolean value) {
        additionalProperties = value;
        additionalPropertiesSchema = null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("additionalProperties")
    public Object getAdditionalPropertiesValue() {
        return additionalPropertiesSchema == null ? additionalProperties : additionalPropertiesSchema;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("additionalProperties")
    public void setAdditionalPropertiesValue(com.fasterxml.jackson.databind.JsonNode value) {
        additionalProperties = value.isBoolean() ? value.booleanValue() : null;
        additionalPropertiesSchema = value.isBoolean() ? null : value;
    }
}
