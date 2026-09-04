package com.ajaxjs.mcp.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents mcp response.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpResponse extends BaseJsonRpcMessage {
    /**
     * Holds the result value.
     */
    private Object result;
}
