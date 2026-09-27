package com.ajaxjs.mcp2026.core;

import lombok.experimental.UtilityClass;

/**
 * Standard codes. Errors use an integer rather than a closed enum so peer and
 * extension errors retain their original code.
 */
@UtilityClass
public class ErrorCodes {
    /**
     * JSON-RPC parse-error code for invalid JSON text.
     */
    public static final int PARSE_ERROR = -32700;

    /**
     * JSON-RPC invalid-request code for an invalid envelope.
     */
    public static final int INVALID_REQUEST = -32600;

    /**
     * JSON-RPC method-not-found code.
     */
    public static final int METHOD_NOT_FOUND = -32601;

    /**
     * JSON-RPC invalid-parameters code, including malformed required request metadata.
     */
    public static final int INVALID_PARAMS = -32602;

    /**
     * JSON-RPC internal-error code.
     */
    public static final int INTERNAL_ERROR = -32603;

    /**
     * MCP 2026-07-28 error code for incompatible HTTP protocol headers.
     */
    public static final int HEADER_MISMATCH = -32020;

    /**
     * MCP 2026-07-28 error code for an undeclared required client capability.
     */
    public static final int MISSING_CLIENT_CAPABILITY = -32021;

    /**
     * MCP 2026-07-28 error code for a request using an unsupported protocol version.
     */
    public static final int UNSUPPORTED_PROTOCOL_VERSION = -32022;
}
