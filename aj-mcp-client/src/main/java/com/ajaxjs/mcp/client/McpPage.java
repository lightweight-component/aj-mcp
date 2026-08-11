package com.ajaxjs.mcp.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** One MCP list page plus the opaque cursor supplied by the remote server. */
@Data
@AllArgsConstructor
public class McpPage<T> {
    private List<T> items;

    private String nextCursor;
}
