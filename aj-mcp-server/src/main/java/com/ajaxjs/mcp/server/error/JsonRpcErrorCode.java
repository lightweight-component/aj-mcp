package com.ajaxjs.mcp.server.error;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents json rpc error code.
 */
public enum JsonRpcErrorCode {
    /**
     * Defines the resource not found constant.
     */
    RESOURCE_NOT_FOUND(-32002),
    /**
     * Defines the internal error constant.
     */
    INTERNAL_ERROR(-32603),
    /**
     * Defines the invalid params constant.
     */
    INVALID_PARAMS(-32602),
    /**
     * Defines the method not found constant.
     */
    METHOD_NOT_FOUND(-32601),
    /**
     * Defines the invalid request constant.
     */
    INVALID_REQUEST(-32600),
    /**
     * Defines the parse error constant.
     */
    PARSE_ERROR(-32700),
    /**
     * Defines the security error constant.
     */
    SECURITY_ERROR(-32001);

    /**
     * Holds the code value.
     */
    private final int code;

    /**
     * Creates a new json rpc error code.
     *
     * @param code the code value.
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