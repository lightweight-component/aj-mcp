package com.ajaxjs.mcp.server.error;

import com.ajaxjs.mcp.common.JsonUtils;

public class JsonRpcErrorException extends RuntimeException {
    /**
     * 具体错误的 Bean，到时序列化为 JSON 用
     */
    private final JsonRpcError jsonRpcError;

    public JsonRpcErrorException(Object id, JsonRpcErrorCode code, String message) {
        super(message);
        this.jsonRpcError = new JsonRpcError(id, code, message);
    }

    public JsonRpcErrorException(Object id, JsonRpcErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.jsonRpcError = new JsonRpcError(id, code, message);
    }

    public JsonRpcErrorException(JsonRpcErrorCode code, String message) {
        super(message);
        this.jsonRpcError = new JsonRpcError(null, code, message);
    }

    public String toJson() {
        return JsonUtils.toJson(jsonRpcError);
    }
}
