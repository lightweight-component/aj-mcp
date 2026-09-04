package com.ajaxjs.mcp.protocol.tools;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequest;
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
    /**
     * Holds the method value.
     */
    String method = TOOLS_CALL;

    /**
     * Holds the params value.
     */
    CallToolRequestParams params;

    /**
     * Creates a new call tool request.
     *
     * @param name the name value.
     */
    public CallToolRequest(String name) {
        this.params = new CallToolRequestParams();
        this.params.setName(name);
    }

    /**
     * Creates a new call tool request.
     *
     * @param name      the name value.
     * @param arguments the arguments value.
     */
    public CallToolRequest(String name, String arguments) {
        this(name, JsonUtils.json2map(arguments));
    }

    /**
     * Creates a new call tool request.
     *
     * @param name      the name value.
     * @param arguments the arguments value.
     */
    public CallToolRequest(String name, Map<String, Object> arguments) {
        this(name);
        this.params.setArguments(arguments);
    }

    /**
     * Creates a new call tool request.
     *
     * @param id        the id value.
     * @param name      the name value.
     * @param arguments the arguments value.
     */
    public CallToolRequest(Long id, String name, Map<String, Object> arguments) {
        this(name, arguments);
        setId(id);
    }
}
