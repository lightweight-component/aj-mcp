package com.foo.myapp.mcp;

/**
 * Provides self-contained binary content for the Spring sample services.
 */
final class SampleMcpContent {
    /** A valid one-pixel PNG encoded as Base64, so the sample needs no external files. */
    static final String TRANSPARENT_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL0WQAAAABJRU5ErkJggg==";

    /** Prevents utility-class instantiation. */
    private SampleMcpContent() {
    }
}
