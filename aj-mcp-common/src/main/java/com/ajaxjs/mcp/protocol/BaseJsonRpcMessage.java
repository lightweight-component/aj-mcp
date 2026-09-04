package com.ajaxjs.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Represents base json rpc message.
 */
@Data
public class BaseJsonRpcMessage {
    /**
     * Defines the version constant.
     */
    public static final String VERSION = "2.0";

    /**
     * 版本
     */
    private final String jsonrpc = VERSION;

    /**
     * id，可以为 null
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Object id;
}
