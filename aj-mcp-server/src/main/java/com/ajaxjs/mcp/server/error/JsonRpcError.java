package com.ajaxjs.mcp.server.error;

import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import lombok.Data;

/**
 * JSON-RPC error response envelope serialized back to the client.
 * <p>
 * Notifications must not produce this object; it is used only when an incoming request has an
 * id and the dispatcher needs to report a protocol, validation, security, or internal error.
 */
@Data
public class JsonRpcError {
    /**
     * JSON-RPC protocol version, always {@code "2.0"}.
     */
    private String jsonrpc = BaseJsonRpcMessage.VERSION;

    /**
     * Request id copied from the failed request so the client can correlate the error.
     */
    private Object id;

    /**
     * Structured error payload containing the numeric code and human-readable message.
     */
    private JsonRpcErrorDetail error;

    /**
     * Creates an error response for a failed JSON-RPC request.
     *
     * @param id      the original request id.
     * @param code    the JSON-RPC error code.
     * @param message the message safe to return to the peer.
     */
    public JsonRpcError(Object id, JsonRpcErrorCode code, String message) {
        this.id = id;
        this.error = new JsonRpcErrorDetail(code, message);
    }
}
