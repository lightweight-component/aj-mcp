package com.ajaxjs.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Long id;
}
