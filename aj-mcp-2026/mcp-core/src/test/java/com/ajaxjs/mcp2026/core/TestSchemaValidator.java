package com.ajaxjs.mcp2026.core;

import com.ajaxjs.mcp2026.core.schema.DefaultSchemaValidator;
import com.ajaxjs.mcp2026.core.schema.SchemaValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static com.ajaxjs.mcp2026.core.TestMcpCodec.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies bounded offline JSON Schema compilation and validation.
 */
class TestSchemaValidator {
    /**
     * Default offline validator used by each schema-validation test.
     */
    private final SchemaValidator validator = new DefaultSchemaValidator();

    @Test
    void validatesRequiredTypesBoundsAndAdditionalProperties() {
        SchemaValidator.CompiledSchema schema = validator.compile(map("type", "object", "properties",
                map("age", map("type", "integer", "minimum", 0)), "required", Arrays.asList("age"), "additionalProperties", false));
        assertTrue(schema.validate(map("age", 7)).isEmpty());
        assertFalse(schema.validate(map("age", -1)).isEmpty());
        assertFalse(schema.validate(map("age", "secret")).isEmpty());
        assertFalse(schema.validate(map()).isEmpty());
        assertFalse(schema.validate(map("age", 1, "extra", true)).isEmpty());
        assertFalse(schema.validate(map("age", "secret")).get(0).getMessage().contains("secret"));
    }

    @Test
    void supportsLocalReferencesCompositionAndUnevaluatedProperties() {
        SchemaValidator.CompiledSchema schema = validator.compile(map("$defs", map("positive", map("type", "number", "exclusiveMinimum", 0)),
                "type", "object", "properties", map("value", map("$ref", "#/$defs/positive")), "unevaluatedProperties", false));
        assertTrue(schema.validate(map("value", 0.5)).isEmpty());
        assertFalse(schema.validate(map("value", 0)).isEmpty());
        assertFalse(schema.validate(map("other", 1)).isEmpty());
        assertFalse(validator.compile(map("oneOf", Arrays.asList(map("type", "number"), map("type", "integer"))))
                .validate(1).isEmpty());
    }

    @Test
    void validatesSchemasAndRejectsUnsupportedDialects() {
        assertThrows(IllegalArgumentException.class, () -> validator.compile(map("type", "not-a-type")));
        assertThrows(IllegalArgumentException.class, () -> validator.compile(map("required", "name")));
        assertThrows(IllegalArgumentException.class, () -> validator.compile(map("$schema", "https://example.org/schema")));
        assertTrue(validator.compile(map("$schema", DefaultSchemaValidator.DRAFT_7, "type", "string")).validate("ok").isEmpty());
    }

    @Test
    void rejectsRemoteFileClasspathAndUnresolvedLocalReferences() {
        for (String ref : Arrays.asList("https://127.0.0.1:1/schema", "file:///secret.json", "classpath:secret.json", "#/$defs/missing"))
            assertThrows(IllegalArgumentException.class, () -> validator.compile(map("$ref", ref)), ref);
    }

    @Test
    void compiledSchemaOwnsSnapshotAndAcceptsScalarNull() {
        Map<String, Object> original = map("type", Arrays.asList("string", "null"));
        SchemaValidator.CompiledSchema schema = validator.compile(original);
        original.put("type", "boolean");
        assertTrue(schema.validate(null).isEmpty());
        assertTrue(schema.validate("ok").isEmpty());
        assertFalse(schema.validate(true).isEmpty());
    }

    @Test
    void rejectsCyclesNonJsonValuesAndStructureLimits() {
        Map<String, Object> cycle = map();
        cycle.put("cycle", cycle);
        assertThrows(IllegalArgumentException.class, () -> validator.compile(cycle));
        SchemaValidator.CompiledSchema schema = new DefaultSchemaValidator(4, 8).compile(map());
        assertThrows(IllegalArgumentException.class, () -> schema.validate(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(new Object()));
    }

    @Test
    void supportsBooleanSubschemasConditionalsArraysAndRecursiveData() {
        SchemaValidator.CompiledSchema arrays = validator.compile(map("type", "array", "prefixItems", Arrays.asList(map("type", "string")), "items", false));
        assertTrue(arrays.validate(Arrays.asList("a")).isEmpty());
        assertFalse(arrays.validate(Arrays.asList("a", 2)).isEmpty());
        SchemaValidator.CompiledSchema conditional = validator.compile(map("if", map("type", "string"), "then", map("minLength", 2), "else", false));
        assertTrue(conditional.validate("ok").isEmpty());
        assertFalse(conditional.validate("x").isEmpty());
        assertFalse(conditional.validate(1).isEmpty());
        SchemaValidator.CompiledSchema recursive = validator.compile(map("type", "object", "properties", map("child", map("$ref", "#"))));
        assertTrue(recursive.validate(map("child", map())).isEmpty());
        assertFalse(recursive.validate(map("child", 1)).isEmpty());
    }

    @Test
    void rejectsNonProgressingRootReferenceCycles() {
        assertThrows(IllegalArgumentException.class, () -> validator.compile(map("$ref", "#")));
        assertThrows(IllegalArgumentException.class, () -> validator.compile(map("allOf", Arrays.asList(map("$ref", "#")))));
    }
}
