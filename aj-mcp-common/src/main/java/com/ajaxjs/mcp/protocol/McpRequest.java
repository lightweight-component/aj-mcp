package com.ajaxjs.mcp.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class McpRequest extends BaseJsonRpcMessage {
    private String method;
}
