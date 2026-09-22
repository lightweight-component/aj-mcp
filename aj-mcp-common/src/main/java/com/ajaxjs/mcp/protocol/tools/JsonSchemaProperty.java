package com.ajaxjs.mcp.protocol.tools;

import lombok.Data;

//import java.util.Map;

/**
 * The properties of Json Schema are actually parameters.
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
     * Holds the description value.
     */
    private String description;

// for complexParameter
//    private Map<String, JsonSchemaProperty> properties;

}
