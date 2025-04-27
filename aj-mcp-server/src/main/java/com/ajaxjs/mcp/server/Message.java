package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class Message {
    public static McpRequest jsonRpcValidate(String inputJson) {
        inputJson = inputJson.trim();
        if (!inputJson.startsWith("{") || !inputJson.endsWith("}")) // 先简单判断一下是否合法的 JSON
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Unable to parse the JSON message");

        JsonNode jsonNode;

        try {
            jsonNode = JsonUtils.OBJECT_MAPPER.readTree(inputJson);
        } catch (IOException e) {
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Unable to parse the JSON message");
        }

        JsonNode jsonrpcNode = jsonNode.get("jsonrpc");

        if (jsonrpcNode == null)
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Empty jsonrpc version.");

        String jsonrpc = jsonrpcNode.asText();

        if (McpUtils.isEmptyText(jsonrpc) || !BaseJsonRpcMessage.VERSION.equals(jsonrpc))
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Invalid jsonrpc version: " + jsonrpc);

        // id 必填
        JsonNode idNode = jsonNode.get("id");

        if (idNode == null)
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Empty id.");

        Long id = idNode.asLong();
        if (id == 0L)
            id = null;

        JsonNode methodNode = jsonNode.get("method");

        if (methodNode == null)
            throw new JsonRpcErrorException(JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found.");

        String method = methodNode.asText();

        if (McpUtils.isEmptyText(method))
            throw new JsonRpcErrorException(JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found.");

        return new McpRequest(id, method, jsonNode);
    }
}
