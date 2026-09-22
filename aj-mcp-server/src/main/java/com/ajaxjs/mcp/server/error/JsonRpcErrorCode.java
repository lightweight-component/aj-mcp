package com.ajaxjs.mcp.server.error;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * JSON-RPC error codes emitted by the server dispatcher and transports.
 * <p>
 * Standard JSON-RPC codes are kept in their reserved ranges, while MCP/server-specific
 * conditions use the implementation-defined {@code -32000} range.
 */
public enum JsonRpcErrorCode {
    /**
     * Requested MCP resource, prompt, tool, or template cannot be found in the current registry.
     */
    RESOURCE_NOT_FOUND(-32002),
    /**
     * Unexpected server-side failure while handling an otherwise valid JSON-RPC request.
     */
    INTERNAL_ERROR(-32603),
    /**
     * Request parameters are missing, malformed, or cannot be converted to the expected Java type.
     */
    INVALID_PARAMS(-32602),
    /**
     * The JSON-RPC method name is not implemented by this server.
     */
    METHOD_NOT_FOUND(-32601),
    /**
     * The JSON-RPC envelope is structurally invalid for a request or notification.
     */
    INVALID_REQUEST(-32600),
    /**
     * The transport could not parse the incoming JSON payload.
     */
    PARSE_ERROR(-32700),
    /**
     * Request was rejected by a transport or policy check, such as an untrusted HTTP Origin.
     */
    SECURITY_ERROR(-32001);

    /**
     * Numeric JSON-RPC code serialized in the {@code error.code} field.
     */
    private final int code;

    /**
     * Creates an enum entry bound to its wire-level integer code.
     *
     * @param code the JSON-RPC error code value.
     */
    JsonRpcErrorCode(int code) {
        this.code = code;
    }

    /**
     * 获取错误码的整数值
     *
     * @return 错误码对应的整数值
     */
    @JsonValue
    public int getCode() {
        return code;
    }

    /**
     * 根据错误码值获取对应的枚举项
     *
     * @param code 错误码值
     * @return 匹配的枚举项
     * @throws IllegalArgumentException 如果没有匹配的枚举项
     */
    public static JsonRpcErrorCode fromCode(int code) {
        for (JsonRpcErrorCode errorCode : JsonRpcErrorCode.values()) {
            if (errorCode.code == code)
                return errorCode;
        }

        throw new IllegalArgumentException("Unknown error code: " + code);
    }
}