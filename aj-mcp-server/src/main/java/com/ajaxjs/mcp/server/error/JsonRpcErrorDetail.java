package com.ajaxjs.mcp.server.error;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents json rpc error detail.
 */
@Data
@AllArgsConstructor
public class JsonRpcErrorDetail {

    /**
     * Holds the code value.
     */
    private JsonRpcErrorCode code;

    /**
     * Holds the message value.
     */
    private String message;
}
