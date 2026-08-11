package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestFeatureBoundaryValidation {
    private McpServer server;

    @BeforeEach
    void setUp() {
        FeatureMgr features = new FeatureMgr();
        features.init("com.ajaxjs.mcp.server.invalidreturn");
        server = new McpServer();
        server.setFeatureMgr(features);
        ServerConfig config = new ServerConfig();
        config.setStrictLifecycle(false);
        server.setServerConfig(config);
    }

    @Test
    void rejectsToolParametersWithoutToolArgDuringScanning() {
        FeatureMgr features = new FeatureMgr();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> features.init("com.ajaxjs.mcp.server.invalidtool"));

        assertTrue(error.getMessage().contains("InvalidToolParameters#missingAnnotation"), error.getMessage());
        assertTrue(error.getMessage().contains("parameter 0"), error.getMessage());
    }

    @Test
    void nullAndInvalidToolListsBecomeToolErrors() {
        assertToolError("nullTool", "Tool returned null");
        assertToolError("invalidToolList", "unsupported value");
    }

    @Test
    void nullAndInvalidPromptListsBecomeInternalErrors() {
        assertInternalError("prompts/get", "{\"name\":\"nullPrompt\"}");
        assertInternalError("prompts/get", "{\"name\":\"invalidPromptList\"}");
    }

    @Test
    void nullAndInvalidResourceListsBecomeInternalErrors() {
        assertInternalError("resources/read", "{\"uri\":\"test:///null\"}");
        assertInternalError("resources/read", "{\"uri\":\"test:///invalid-list\"}");
    }

    private void assertToolError(String name, String message) {
        String json = response("tools/call", "{\"name\":\"" + name + "\",\"arguments\":{}}");
        assertTrue(json.contains("\"isError\":true"), json);
        assertTrue(json.contains(message), json);
    }

    private void assertInternalError(String method, String params) {
        JsonRpcErrorException error = assertThrows(JsonRpcErrorException.class,
                () -> process(method, params));
        assertTrue(error.toJson().contains("\"code\":" + JsonRpcErrorCode.INTERNAL_ERROR.getCode()));
    }

    private String response(String method, String params) {
        return JsonUtils.toJson(process(method, params));
    }

    private McpResponse process(String method, String params) {
        return server.processMessage(McpServerInitialize.jsonRpcValidate(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method
                        + "\",\"params\":" + params + "}"));
    }
}
