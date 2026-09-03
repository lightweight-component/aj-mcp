package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.utils.RequestMeta;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

import static com.ajaxjs.mcp.protocol.McpConstant.Methods.TOOLS_CALL;

/**
 * Calling Tools
 * To invoke a tool, clients send a tools/call request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallToolRequest extends McpRequest {
    String method = TOOLS_CALL;

    CallToolRequestParams params;

    public CallToolRequest(String name) {
        this.params = new CallToolRequestParams();
        this.params.setName(name);
    }

    public CallToolRequest(String name, String arguments) {
        this(name, JsonUtils.json2map(arguments));
    }

    public CallToolRequest(String name, Map<String, Object> arguments) {
        this(name);
        this.params.setArguments(arguments);
    }

    public CallToolRequest(Long id, String name, Map<String, Object> arguments) {
        this(name, arguments);
        setId(id);
    }
}
