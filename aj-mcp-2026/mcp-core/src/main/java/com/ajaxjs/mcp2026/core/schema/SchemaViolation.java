package com.ajaxjs.mcp2026.core.schema;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Validation diagnostic without embedding the failing value or entire payload.
 */
@Getter
@RequiredArgsConstructor
public final class SchemaViolation {
    /**
     * JSON Pointer-like location of the invalid instance value.
     */
    @NonNull
    private final String instancePath;

    /**
     * JSON Schema keyword whose assertion failed.
     */
    @NonNull
    private final String keyword;

    /**
     * Safe diagnostic text that does not need to include the rejected value.
     */
    @NonNull
    private final String message;
}
