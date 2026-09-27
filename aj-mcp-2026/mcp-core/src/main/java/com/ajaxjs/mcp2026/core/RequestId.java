package com.ajaxjs.mcp2026.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * String or exact integer ID. Numeric and textual IDs never compare equal.
 */
@Getter
@EqualsAndHashCode
public final class RequestId {
    /**
     * Immutable wire value, either a string or a {@link BigInteger}.
     */
    private final Object value;

    /**
     * Creates an already validated request identifier.
     *
     * @param value string or exact integer wire value
     */
    private RequestId(Object value) {
        this.value = value;
    }

    /**
     * Creates a string JSON-RPC request identifier.
     *
     * @param value non-null string ID
     * @return typed request identifier
     */
    public static RequestId of(String value) {
        return new RequestId(Objects.requireNonNull(value, "id"));
    }

    /**
     * Creates an exact integer JSON-RPC request identifier.
     *
     * @param value integer ID
     * @return typed request identifier
     */
    public static RequestId of(long value) {
        return new RequestId(BigInteger.valueOf(value));
    }

    /**
     * Converts a decoded JSON string or exact integer to a request identifier.
     *
     * @param value decoded ID value
     * @return typed request identifier
     */
    public static RequestId from(Object value) {
        if (value instanceof String)
            return of((String) value);

        if (value instanceof BigInteger)
            return new RequestId(value);

        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
            return of(((Number) value).longValue());

        if (value instanceof BigDecimal) {
            try {
                return new RequestId(((BigDecimal) value).toBigIntegerExact());
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Request id must be an integer", e);
            }
        }

        throw new IllegalArgumentException("Request id must be a string or exact integer");
    }

}
