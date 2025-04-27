package com.ajaxjs.mcp.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class McpResponse extends BaseJsonRpcMessage {
    private Object result;
}
