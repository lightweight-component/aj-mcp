package com.ajaxjs.mcp.server.error;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import lombok.Data;

/**
 * Represents json rpc error.
 */
@Data
public class JsonRpcError {
    /**
     * Holds the jsonrpc value.
     */
    private String jsonrpc = BaseJsonRpcMessage.VERSION;

    /**
     * Holds the id value.
     */
    private Object id;

    /**
     * Holds the error value.
     */
    private JsonRpcErrorDetail error;

    /**
     * Creates a new json rpc error.
     *
     * @param id      the id value.
     * @param code    the code value.
     * @param message the message value.
     */
    public JsonRpcError(Object id, JsonRpcErrorCode code, String message) {
        this.id = id;
        this.error = new JsonRpcErrorDetail(code, message);
    }
}
