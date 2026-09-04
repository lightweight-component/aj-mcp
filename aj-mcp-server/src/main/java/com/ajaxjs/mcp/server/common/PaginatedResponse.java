package com.ajaxjs.mcp.server.common;

import com.ajaxjs.mcp.common.McpUtils;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * A class representing the paginated response.
 *
 * @param <T> The type of the items in the list.
 */
@AllArgsConstructor
public class PaginatedResponse<T> {
    /**
     * Holds the list value.
     */
    private final List<T> list;
    /**
     * Holds the is last page value.
     */
    private final boolean isLastPage;
    /**
     * Holds the next page no value.
     */
    private final Integer nextPageNo;

    /**
     * Executes the get list operation.
     *
     * @return the result of the get list operation.
     */
    public List<T> getList() {
        return list;
    }

    /**
     * Executes the is last page operation.
     *
     * @return the result of the is last page operation.
     */
    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Executes the get next page no operation.
     *
     * @return the result of the get next page no operation.
     */
    public Integer getNextPageNo() {
        return nextPageNo;
    }

    /**
     * Executes the get next page no as bse64 operation.
     *
     * @return the result of the get next page no as bse64 operation.
     */
    public String getNextPageNoAsBse64() {
        return McpUtils.base64Encode(String.format("{\"page\":%d}", nextPageNo));
    }
}