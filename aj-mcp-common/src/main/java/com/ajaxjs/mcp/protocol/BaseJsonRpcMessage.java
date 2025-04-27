package com.ajaxjs.mcp.protocol;

import lombok.Data;

@Data
public class BaseJsonRpcMessage {
    public static final String VERSION = "2.0";

    /**
     * 版本
     */
    private final String jsonrpc = VERSION;

    /**
     * id，可以为 null
     */
    protected Long id;
}
