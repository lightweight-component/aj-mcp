package com.ajaxjs.mcp.server.common;

import com.ajaxjs.mcp.common.McpUtils;

import java.util.List;

/**
 * A class representing the paginated response.
 *
 * @param <T> The type of the items in the list.
 */
public class PaginatedResponse<T> {
    private final List<T> list;
    private final boolean isLastPage;
    private final Integer nextPageNo;

    public PaginatedResponse(List<T> list, boolean isLastPage, Integer nextPageNo) {
        this.list = list;
        this.isLastPage = isLastPage;
        this.nextPageNo = nextPageNo;
    }

    public List<T> getList() {
        return list;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public Integer getNextPageNo() {
        return nextPageNo;
    }

    public String getNextPageNoAsBse64() {
        return McpUtils.base64Encode(String.format("{\"page\":%d}", nextPageNo));
    }
}