package com.ajaxjs.mcp.protocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MCP protocol revisions implemented by this SDK.
 *
 * <p>The enum centralizes revision-dependent behavior. Comparing version strings
 * throughout the client and server is deliberately avoided so that adding a new
 * revision has one well-defined compatibility boundary.</p>
 */
public enum ProtocolVersion {
    V_2024_11_05("2024-11-05", false, false, false),
    V_2025_03_26("2025-03-26", true, false, false),
    V_2025_06_18("2025-06-18", true, true, true);

    public static final ProtocolVersion LATEST = V_2025_06_18;

    private final String value;
    private final boolean streamableHttp;
    private final boolean structuredToolOutput;
    private final boolean elicitation;

    ProtocolVersion(String value, boolean streamableHttp, boolean structuredToolOutput, boolean elicitation) {
        this.value = value;
        this.streamableHttp = streamableHttp;
        this.structuredToolOutput = structuredToolOutput;
        this.elicitation = elicitation;
    }

    public String value() {
        return value;
    }

    public boolean supportsStreamableHttp() {
        return streamableHttp;
    }

    public boolean supportsStructuredToolOutput() {
        return structuredToolOutput;
    }

    public boolean supportsElicitation() {
        return elicitation;
    }

    public static ProtocolVersion from(String value) {
        for (ProtocolVersion version : values()) {
            if (version.value.equals(value))
                return version;
        }
        throw new IllegalArgumentException("Unsupported MCP protocol version: " + value);
    }

    public static boolean isSupported(String value) {
        try {
            from(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Newest-first order is useful for client preference and server fallback.
     */
    public static List<String> supportedVersions() {
        return Collections.unmodifiableList(Arrays.asList(
                V_2025_06_18.value, V_2025_03_26.value, V_2024_11_05.value));
    }
}
