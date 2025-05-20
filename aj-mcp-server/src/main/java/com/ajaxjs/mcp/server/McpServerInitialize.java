package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.BaseJsonRpcMessage;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.ajaxjs.mcp.protocol.initialize.InitializeResponse;
import com.ajaxjs.mcp.protocol.initialize.InitializeResponseResult;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public abstract class McpServerInitialize implements McpConstant {
    ServerConfig serverConfig;

    McpTransportSync transport;

    /**
     * Retrieves a store object by name from the given map.
     * This method is used to fetch a specific store object by its name from a map of string to generic type T.
     * It first checks if the map is null, and throws an exception if so.
     * Then it attempts to get the store object from the map. If the object with the specified name is not found, another exception is thrown.
     * If the store object is found, it is returned to the caller.
     *
     * @param map  The map from String to objects of generic type T, representing the collection of stores
     * @param name The name of the store to retrieve
     * @return the store object of type T; if the map is empty or the store is not found, an exception is thrown
     * @throws NullPointerException if the map is null or the store with the specified name is not found
     */
    static <T> T getStore(Map<String, T> map, String name) {
        if (map.isEmpty())
            throw new NullPointerException("Store is NOT in initialized");

        T store = map.get(name);

        if (store == null)
            throw new NullPointerException("The " + name + " store is NOT found.");

        return store;
    }

    /**
     * Initializes the connection with the client and returns the server configuration information.
     * <p>
     * This method receives a client ID and request parameters in JsonNode format, converts the JsonNode to an InitializeRequest object,
     * processes the requested protocol version, and returns the server information and capabilities in InitializeResponse.
     *
     * @param id       Client request identifier
     * @param jsonNode Client request parameters in JsonNode format
     * @return Returns the initialization response object containing the server configuration information
     */
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
        serverInfo.setVersion(serverConfig.getVersion());
        serverInfo.setName(serverConfig.getName());

        InitializeResponseResult.Capabilities.Tools tools = new InitializeResponseResult.Capabilities.Tools();
        tools.setListChanged(true);

        InitializeResponseResult.Capabilities capabilities = new InitializeResponseResult.Capabilities();
        capabilities.setTools(tools);

        InitializeResponseResult result = new InitializeResponseResult();
        result.setProtocolVersion(serverProtocolVersion);
        result.setServerInfo(serverInfo);
        result.setCapabilities(capabilities);

        InitializeResponse resp = new InitializeResponse();
        resp.setId(id);
        resp.setResult(result);

        return resp;
    }

    /**
     * Validates and parses a JSON-RPC request.
     * This method is primarily used to verify that the input JSON string conforms to the JSON-RPC specification,
     * and to extract the necessary information from the request.
     *
     * @param inputJson The input JSON string, which should contain complete JSON-RPC request information
     * @return An McpRequestRawInfo object containing the request ID, method name, and raw JSON data
     * @throws JsonRpcErrorException If the input JSON string does not conform to the specification or is missing required fields, this exception is thrown
     */
    static McpRequestRawInfo jsonRpcValidate(String inputJson) {
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
        JsonNode idNode = jsonNode.get(ID);

        if (idNode == null)
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Empty id.");

        Long id = idNode.asLong();
        if (id == 0L)
            id = null;

        JsonNode methodNode = jsonNode.get(METHOD);

        if (methodNode == null)
            throw new JsonRpcErrorException(JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found.");

        String method = methodNode.asText();

        if (McpUtils.isEmptyText(method))
            throw new JsonRpcErrorException(JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found.");

        return new McpRequestRawInfo(id, method, jsonNode);
    }
}
