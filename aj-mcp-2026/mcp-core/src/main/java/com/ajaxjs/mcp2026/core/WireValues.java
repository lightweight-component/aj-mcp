package com.ajaxjs.mcp2026.core;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Owns protocol payload snapshots. This is a wire-boundary validation, not a
 * general bean converter; callers can use AJ Util to convert beans beforehand.
 */
@UtilityClass
class WireValues {

    /**
     * Validates and deeply snapshots a JSON object value.
     *
     * @param value candidate map value
     * @param field logical field name for diagnostics
     * @return immutable snapshot with string keys and JSON-compatible descendants
     * @throws IllegalArgumentException if {@code value} is not a valid JSON object
     */
    static Map<String, Object> object(Object value, String field) {
        if (!(value instanceof Map))
            throw new IllegalArgumentException(field + " must be an object");

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot = (Map<String, Object>) copy(value, 0);

        return snapshot;
    }

    /**
     * Validates one JSON string field.
     *
     * @param value candidate value
     * @param field logical field name for diagnostics
     * @return the supplied string value
     * @throws IllegalArgumentException if the value is not a string
     */
    static String string(Object value, String field) {
        if (!(value instanceof String))
            throw new IllegalArgumentException(field + " must be a string");

        return (String) value;
    }

    /**
     * Deeply snapshots a JSON-compatible value while bounding structural depth.
     *
     * @param value value to copy
     * @param depth current recursive depth
     * @return immutable maps/lists or a validated scalar
     * @throws IllegalArgumentException if the value is unsupported, non-finite, or too deeply nested
     */
    static Object copy(Object value, int depth) {
        if (depth > 64)
            throw new IllegalArgumentException("JSON nesting exceeds 64 levels");

        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof BigInteger || value instanceof BigDecimal
                || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long)
            return value;

        if (value instanceof Float || value instanceof Double) {
            if (!Double.isFinite(((Number) value).doubleValue()))
                throw new IllegalArgumentException("JSON numbers must be finite");

            return value;
        }

        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet())
                result.put(string(entry.getKey(), "Object key"), copy(entry.getValue(), depth + 1));

            return Collections.unmodifiableMap(result);
        }

        if (value instanceof List) {
            List<Object> result = new ArrayList<>();

            for (Object element : (List<?>) value)
                result.add(copy(element, depth + 1));

            return Collections.unmodifiableList(result);
        }

        throw new IllegalArgumentException("Expected a JSON value (Map, List, scalar or null)");
    }

    /**
     * Creates the mandatory base members of a JSON-RPC 2.0 envelope.
     *
     * @return mutable envelope map containing only {@code jsonrpc=2.0}
     */
    static Map<String, Object> envelope() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("jsonrpc", "2.0");

        return value;
    }
}
