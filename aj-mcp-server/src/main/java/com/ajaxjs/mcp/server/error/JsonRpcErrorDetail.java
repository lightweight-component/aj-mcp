package com.ajaxjs.mcp.server.error;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Payload stored in the {@code error} member of a JSON-RPC error response.
 */
@Data
@AllArgsConstructor
public class JsonRpcErrorDetail {

    /**
     * Machine-readable JSON-RPC error code.
     */
    private JsonRpcErrorCode code;

    /**
     * Human-readable diagnostic message returned to the peer.
     */
    private String message;
}
