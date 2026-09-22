package com.ajaxjs.mcp.server.error;

import com.ajaxjs.mcp.common.JsonUtils;

/**
 * Runtime exception that carries a ready-to-serialize JSON-RPC error response.
 * <p>
 * Dispatcher code throws this exception for expected protocol and validation failures so
 * transports can return structured errors while preserving the original Java cause when present.
 */
public class JsonRpcErrorException extends RuntimeException {
    /**
     * Structured error envelope serialized when the transport needs to reply to the client.
     */
    private final JsonRpcError jsonRpcError;

    /**
     * Creates an exception for a failed request with a known request id.
     *
     * @param id      original JSON-RPC request id.
     * @param code    error code to serialize.
     * @param message diagnostic message to return to the peer.
     */
    public JsonRpcErrorException(Object id, JsonRpcErrorCode code, String message) {
        super(message);
        this.jsonRpcError = new JsonRpcError(id, code, message);
    }

    /**
     * Creates an exception that preserves the Java cause while returning a protocol error.
     *
     * @param id      original JSON-RPC request id.
     * @param code    error code to serialize.
     * @param message diagnostic message to return to the peer.
     * @param cause   underlying failure for local logging or inspection.
     */
    public JsonRpcErrorException(Object id, JsonRpcErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.jsonRpcError = new JsonRpcError(id, code, message);
    }

    /**
     * Creates an exception for an error response without a usable request id, such as parse errors.
     *
     * @param code    error code to serialize.
     * @param message diagnostic message to return to the peer.
     */
    public JsonRpcErrorException(JsonRpcErrorCode code, String message) {
        super(message);
        this.jsonRpcError = new JsonRpcError(null, code, message);
    }

    /**
     * Serializes the embedded JSON-RPC error envelope.
     *
     * @return JSON text suitable for writing as a protocol response.
     */
    public String toJson() {
        return JsonUtils.toJson(jsonRpcError);
    }
}
