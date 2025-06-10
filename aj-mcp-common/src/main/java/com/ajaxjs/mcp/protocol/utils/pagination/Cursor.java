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
    private String cursor;

    @JsonIgnore
    private Integer pageNo;

    public void setCursor(String cursor) {
        String json = McpUtils.base64Decode(cursor);
        this.cursor = json;
        Map<String, Object> map = JsonUtils.json2map(json);
        Object page = map.get("page");

        if (page == null)
            throw new IllegalArgumentException("The params of pagination 'page' is required.");

        this.pageNo = (Integer) page;
    }

    public Cursor(int pageNo) {
        this.pageNo = pageNo;
        String json = "{\"page\":" + pageNo + "}";
        cursor = McpUtils.base64Encode(json);
    }
}
