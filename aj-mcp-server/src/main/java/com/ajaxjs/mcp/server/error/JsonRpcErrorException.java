package com.ajaxjs.mcp.server.error;

import com.ajaxjs.mcp.common.JsonUtils;

/**
 * Represents json rpc error exception.
 */
public class JsonRpcErrorException extends RuntimeException {
    /**
     * 具体错误的 Bean，到时序列化为 JSON 用
     */
    private final JsonRpcError jsonRpcError;

    /**
     * Creates a new json rpc error exception.
     *
     * @param id      the id value.
     * @param code    the code value.
     * @param message the message value.
     */
    public JsonRpcErrorException(Object id, JsonRpcErrorCode code, String message) {
        super(message);
        this.jsonRpcError = new JsonRpcError(id, code, message);
    }

    /**
     * Creates a new json rpc error exception.
     *
     * @param id      the id value.
     * @param code    the code value.
     * @param message the message value.
     * @param cause   the cause value.
     */
    public JsonRpcErrorException(Object id, JsonRpcErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.jsonRpcError = new JsonRpcError(id, code, message);
    }

    /**
     * Creates a new json rpc error exception.
     *
     * @param code    the code value.
     * @param message the message value.
     */
    public JsonRpcErrorException(JsonRpcErrorCode code, String message) {
        super(message);
        this.jsonRpcError = new JsonRpcError(null, code, message);
    }

    /**
     * Executes the to json operation.
     *
     * @return the result of the to json operation.
     */
    public String toJson() {
        return JsonUtils.toJson(jsonRpcError);
    }
}
