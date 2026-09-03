package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.protocol.common.Content;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * A Java return type for MCP 2025-06-18 structured tool results.
 * Text/content remains present for compatibility with clients that do not consume
 * {@code structuredContent}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructuredToolResult {
    private List<Content> content;

    private Map<String, Object> structuredContent;

    private boolean error;
}
