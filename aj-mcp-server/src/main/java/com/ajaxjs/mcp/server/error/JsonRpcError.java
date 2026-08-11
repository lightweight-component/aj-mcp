package com.ajaxjs.mcp.server.error;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import lombok.Data;

@Data
public class JsonRpcError {
    private String jsonrpc = BaseJsonRpcMessage.VERSION;

    private Object id;

    private JsonRpcErrorDetail error;

    public JsonRpcError(Object id, JsonRpcErrorCode code, String message) {
        this.id = id;
        this.error = new JsonRpcErrorDetail(code, message);
    }
}
