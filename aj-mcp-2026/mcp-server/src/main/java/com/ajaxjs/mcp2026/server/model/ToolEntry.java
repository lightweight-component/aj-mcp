package com.ajaxjs.mcp2026.server.model;

import com.ajaxjs.mcp2026.core.ToolContract;
import com.ajaxjs.mcp2026.core.ToolDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Immutable registration joining a public tool descriptor, handler, and executable schema contract.
 */
@Getter
@RequiredArgsConstructor
public class ToolEntry {
    /**
     * Full immutable tool descriptor exposed through {@code tools/list}.
     */
    final ToolDefinition definition;

    /**
     * Application handler invoked for {@code tools/call}.
     */
    final Handler handler;

    /**
     * Input/output schema contract enforced around handler invocation.
     */
    final ToolContract contract;
}
