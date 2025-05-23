package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Data
@Slf4j
public class ServerSse implements McpTransportSync {
    private McpServer server;

    public ServerSse(McpServer server) {
        this.server = server;
    }
    @Override
    public void start() {
    }

    @Override
    public String handle(String rawJson) {
        McpRequestRawInfo request = McpServerInitialize.jsonRpcValidate(rawJson); // 解析输入消息
        McpResponse mcpResponse = server.processMessage(request);

        return JsonUtils.toJson(mcpResponse);  // 处理消息并生成响应
    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() throws IOException {

    }
}
