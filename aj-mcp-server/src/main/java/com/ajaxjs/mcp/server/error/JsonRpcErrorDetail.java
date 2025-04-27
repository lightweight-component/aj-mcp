package com.ajaxjs.mcp.server.error;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JsonRpcErrorDetail {

    private JsonRpcErrorCode code;

    private String message;
}
