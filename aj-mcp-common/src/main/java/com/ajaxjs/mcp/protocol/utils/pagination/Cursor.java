package com.ajaxjs.mcp.protocol.utils.pagination;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Pagination
 */
@Data
@NoArgsConstructor
public class Cursor {
    /**
     * Holds the cursor value.
     */
    private String cursor;

    /**
     * Holds the page no value.
     */
    @JsonIgnore
    private Integer pageNo;

    /**
     * Executes the set cursor operation.
     *
     * @param cursor the cursor value.
     */
    public void setCursor(String cursor) {
        String json = McpUtils.base64Decode(cursor);
        this.cursor = json;
        Map<String, Object> map = JsonUtils.json2map(json);
        Object page = map.get("page");

        if (page == null)
            throw new IllegalArgumentException("The params of pagination 'page' is required.");

        this.pageNo = (Integer) page;
    }

    /**
     * Creates a new cursor.
     *
     * @param pageNo the page no value.
     */
    public Cursor(int pageNo) {
        this.pageNo = pageNo;
        String json = "{\"page\":" + pageNo + "}";
        cursor = McpUtils.base64Encode(json);
    }

    /**
     * Creates a client-side cursor without interpreting the server's opaque value.
     *
     * @param opaqueCursor the opaque cursor value supplied by the server.
     */
    public Cursor(String opaqueCursor) {
        this.cursor = opaqueCursor;
    }
}
