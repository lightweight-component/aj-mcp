package com.ajaxjs.mcp.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * One page returned by an MCP list operation.
 *
 * <p>{@link #items} contains only the entries in this page. The
 * {@link #nextCursor} value is an opaque server token: clients must not parse,
 * manufacture, or persist assumptions about its format. Pass it unchanged to
 * the next page request; a null value indicates that pagination is complete.</p>
 */
@Data
@AllArgsConstructor
public class McpPage<T> {
    /**
     * Items contained in this page, in the order returned by the server.
     */
    private List<T> items;

    /**
     * Opaque cursor for the next page, or null when this is the final page.
     */
    private String nextCursor;
}
