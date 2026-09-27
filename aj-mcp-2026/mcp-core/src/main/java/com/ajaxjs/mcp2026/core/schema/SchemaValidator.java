package com.ajaxjs.mcp2026.core.schema;

import java.util.List;
import java.util.Map;

/**
 * Extension boundary for a Java 8 compatible JSON Schema validator.
 * Implementations must support 2020-12, honor an explicit $schema dialect,
 * reject invalid/unresolved schemas, bound validation work, and disable remote
 * reference fetching by default. DefaultSchemaValidator is the offline implementation.
 */
public interface SchemaValidator {
    /**
     * Default JSON Schema dialect when a schema omits its {@code $schema} declaration.
     */
    String DEFAULT_DIALECT = "https://json-schema.org/draft/2020-12/schema";

    /**
     * Compiles and validates the schema itself; an invalid or unsupported schema
     * must throw, never become an always-successful validator.
     *
     * @param schema complete schema object, including unknown keywords
     * @return reusable compiled validation plan
     */
    CompiledSchema compile(Map<String, Object> schema);

    /**
     * Reusable validation plan produced from one valid schema and dialect.
     */
    interface CompiledSchema {
        /**
         * @param instance JSON Map/List/scalar/null value
         * @return violations; empty only when the instance is valid
         */
        List<SchemaViolation> validate(Object instance);
    }
}
