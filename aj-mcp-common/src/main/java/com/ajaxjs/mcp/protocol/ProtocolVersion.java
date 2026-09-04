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
    /**
     * Defines the v 2024 11 05 constant.
     */
    V_2024_11_05("2024-11-05", false, false, false),
    /**
     * Defines the v 2025 03 26 constant.
     */
    V_2025_03_26("2025-03-26", true, false, false),
    /**
     * Defines the v 2025 06 18 constant.
     */
    V_2025_06_18("2025-06-18", true, true, true);

    /**
     * Defines the latest constant.
     */
    public static final ProtocolVersion LATEST = V_2025_06_18;

    /**
     * Holds the value value.
     */
    private final String value;
    /**
     * Holds the streamable http value.
     */
    private final boolean streamableHttp;
    /**
     * Holds the structured tool output value.
     */
    private final boolean structuredToolOutput;
    /**
     * Holds the elicitation value.
     */
    private final boolean elicitation;

    /**
     * Creates a new protocol version.
     *
     * @param value                the value value.
     * @param streamableHttp       the streamable http value.
     * @param structuredToolOutput the structured tool output value.
     * @param elicitation          the elicitation value.
     */
    ProtocolVersion(String value, boolean streamableHttp, boolean structuredToolOutput, boolean elicitation) {
        this.value = value;
        this.streamableHttp = streamableHttp;
        this.structuredToolOutput = structuredToolOutput;
        this.elicitation = elicitation;
    }

    /**
     * Executes the value operation.
     *
     * @return the result of the value operation.
     */
    public String value() {
        return value;
    }

    /**
     * Executes the supports streamable http operation.
     *
     * @return the result of the supports streamable http operation.
     */
    public boolean supportsStreamableHttp() {
        return streamableHttp;
    }

    /**
     * Executes the supports structured tool output operation.
     *
     * @return the result of the supports structured tool output operation.
     */
    public boolean supportsStructuredToolOutput() {
        return structuredToolOutput;
    }

    /**
     * Executes the supports elicitation operation.
     *
     * @return the result of the supports elicitation operation.
     */
    public boolean supportsElicitation() {
        return elicitation;
    }

    /**
     * Executes the from operation.
     *
     * @param value the value value.
     * @return the result of the from operation.
     */
    public static ProtocolVersion from(String value) {
        for (ProtocolVersion version : values()) {
            if (version.value.equals(value))
                return version;
        }

        throw new IllegalArgumentException("Unsupported MCP protocol version: " + value);
    }

    /**
     * Executes the is supported operation.
     *
     * @param value the value value.
     * @return the result of the is supported operation.
     */
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
     *
     * @return the supported protocol versions, ordered from newest to oldest.
     */
    public static List<String> supportedVersions() {
        return Collections.unmodifiableList(Arrays.asList(V_2025_06_18.value, V_2025_03_26.value, V_2024_11_05.value));
    }
}
