package com.ajaxjs.mcp.server.common;

import com.ajaxjs.mcp.common.McpUtils;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Immutable page result used by MCP list operations before it is converted into a
 * protocol-specific response object.
 *
 * @param <T> the type of the items contained in the current page.
 */
@AllArgsConstructor
public class PaginatedResponse<T> {
    /**
     * Items selected for the current page. The list reference is supplied by the caller and
     * should be treated as read-only by response builders.
     */
    private final List<T> list;
    /**
     * Whether this page reaches the end of the source collection.
     */
    private final boolean isLastPage;
    /**
     * One-based page number to encode as the next cursor, or {@code null} when there is no next page.
     */
    private final Integer nextPageNo;

    /**
     * Returns the items selected for this page.
     *
     * @return the current page items.
     */
    public List<T> getList() {
        return list;
    }

    /**
     * Indicates whether no further page is available.
     *
     * @return {@code true} when this page is the last page.
     */
    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Returns the one-based page number for the next cursor.
     *
     * @return the next page number, or {@code null} when this page is final.
     */
    public Integer getNextPageNo() {
        return nextPageNo;
    }

    /**
     * Encodes the next page number as the opaque base64 cursor used by the MCP list APIs.
     *
     * @return a base64 cursor containing the next page number.
     */
    public String getNextPageNoAsBse64() {
        return McpUtils.base64Encode(String.format("{\"page\":%d}", nextPageNo));
    }
}