package com.ajaxjs.mcp.server.error;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonRpcErrorCode {
    RESOURCE_NOT_FOUND(-32002),
    INTERNAL_ERROR(-32603),
    INVALID_PARAMS(-32602),
    METHOD_NOT_FOUND(-32601),
    INVALID_REQUEST(-32600),
    PARSE_ERROR(-32700),
    SECURITY_ERROR(-32001);

    private final int code;

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