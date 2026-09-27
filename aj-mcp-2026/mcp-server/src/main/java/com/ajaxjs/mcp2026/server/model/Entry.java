package com.ajaxjs.mcp2026.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Immutable registration pairing a discoverable resource or prompt descriptor with its handler.
 */
@RequiredArgsConstructor
@Getter
public class Entry {
    /**
     * Immutable resource or prompt descriptor retained for discovery and invocation.
     */
    final Map<String, Object> descriptor;

    /**
     * Application handler bound to the descriptor.
     */
    final Handler handler;
}
