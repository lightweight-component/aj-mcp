package com.ajaxjs.mcp.protocol.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata shared by MCP requests that support progress reporting.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestMeta {
    /**
     * Holds the progress token value.
     */
    private Object progressToken;
}
