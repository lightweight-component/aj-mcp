package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.ajaxjs.mcp.protocol.initialize.InitializeResponse;
import com.ajaxjs.mcp.protocol.initialize.InitializeResponseResult;
import com.ajaxjs.mcp.server.model.ServerConfig;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public abstract class McpServerInitialize implements McpConstant {
    ServerConfig serverConfig;

    McpTransportSync transport;

    McpResponse initialize(Long id, JsonNode jsonNode) {
        InitializeRequest initializeRequest;

        try {
            initializeRequest = JsonUtils.OBJECT_MAPPER.treeToValue(jsonNode, InitializeRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("JsonNode converts to bean.", e);
            throw new RuntimeException(e);
        }

        InitializeRequestParams requestParams = initializeRequest.getParams();
        List<String> protocolVersions = serverConfig.getProtocolVersions();
        /*
            The server MUST respond with the highest protocol version it supports
            if it does not support the requested (e.g. Client) version.
         */
        String serverProtocolVersion = protocolVersions.get(protocolVersions.size() - 1);

        /*
            If the server supports the requested protocol version, it MUST respond with the same version.
         */
        if (protocolVersions.contains(requestParams.getProtocolVersion()))
            serverProtocolVersion = requestParams.getProtocolVersion();
        else
            log.warn("Client requested unsupported protocol version: {}, so the server will suggest the {} version instead",
                    requestParams.getProtocolVersion(), serverProtocolVersion);

        InitializeResponseResult.ServerInfo serverInfo = new InitializeResponseResult.ServerInfo();
        serverInfo.setVersion(serverProtocolVersion);
        serverInfo.setName(serverInfo.getName());

        InitializeResponseResult result = new InitializeResponseResult();
        result.setServerInfo(serverInfo);

        InitializeResponse resp = new InitializeResponse();
        resp.setId(id);
        resp.setResult(result);

        return resp;
    }
}
