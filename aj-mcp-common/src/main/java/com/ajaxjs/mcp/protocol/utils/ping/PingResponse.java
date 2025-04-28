package com.ajaxjs.mcp.protocol.utils.ping;

import com.ajaxjs.mcp.protocol.McpResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class PingResponse extends McpResponse {
    private final Map<String, Object> result = new HashMap<>();
}
