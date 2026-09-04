package com.ajaxjs.mcp.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents mcp request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpRequest extends BaseJsonRpcMessage {
    /**
     * Holds the method value.
     */
    private String method;
}
