package com.ajaxjs.mcp2026.core;

import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;

/**
 * Versions implemented by the modern core, independent of the legacy SDK.
 */
@UtilityClass
public class ProtocolVersion {
    /**
     * The MCP revision implemented by this module.
     */
    public static final String V_2026_07_28 = "2026-07-28";

    /**
     * Immutable protocol revisions accepted after a request have been decoded.
     */
    public static final List<String> SUPPORTED = Collections.singletonList(V_2026_07_28);

}
