package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.*;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams;
import com.ajaxjs.mcp.protocol.initialize.InitializeResponse;
import com.ajaxjs.mcp.protocol.initialize.InitializeResponseResult;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Represents mcp server initialize.
 */
@Slf4j
@Data
public abstract class McpServerInitialize implements McpConstant {
    /**
     * Holds the feature mgr value.
     */
    FeatureMgr featureMgr = new FeatureMgr();

    /**
     * Holds the server config value.
     */
    ServerConfig serverConfig;

    /**
     * Holds the transport value.
     */
    McpTransportSync transport;

    /**
     * Executes the get store operation.
     *
     * @param <T>         the t type.
     * @param map         the map value.
     * @param name        the name value.
     * @param requestId   the request id value.
     * @param featureType the feature type value.
     * @return the result of the get store operation.
     */
    static <T> T getStore(Map<String, T> map, String name, Object requestId, String featureType) {
        if (McpUtils.isEmptyText(name))
            throw new JsonRpcErrorException(requestId, JsonRpcErrorCode.INVALID_PARAMS,
                    featureType + " name is required");

        T store = map.get(name);
        if (store == null)
            throw new JsonRpcErrorException(requestId, JsonRpcErrorCode.INVALID_PARAMS,
                    "Unknown " + featureType + ": " + name);

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
    McpResponse initialize(Object id, JsonNode jsonNode) {
        JsonNode paramsNode = jsonNode == null ? null : jsonNode.get(PARAMS);

        if (paramsNode == null || !paramsNode.isObject())
            throw new JsonRpcErrorException(id, JsonRpcErrorCode.INVALID_PARAMS,
                    "initialize params must be an object");

        JsonNode protocolVersionNode = paramsNode.get("protocolVersion");
        if (protocolVersionNode == null || !protocolVersionNode.isTextual()
                || McpUtils.isEmptyText(protocolVersionNode.textValue()))
            throw new JsonRpcErrorException(id, JsonRpcErrorCode.INVALID_PARAMS,
                    "protocolVersion must be a non-empty string");

        JsonNode capabilitiesNode = paramsNode.get("capabilities");
        if (capabilitiesNode == null || !capabilitiesNode.isObject())
            throw new JsonRpcErrorException(id, JsonRpcErrorCode.INVALID_PARAMS,
                    "capabilities must be an object");

        JsonNode clientInfoNode = paramsNode.get("clientInfo");
        if (clientInfoNode == null || !clientInfoNode.isObject()
                || !nonEmptyText(clientInfoNode.get("name")) || !nonEmptyText(clientInfoNode.get("version")))
            throw new JsonRpcErrorException(id, JsonRpcErrorCode.INVALID_PARAMS,
                    "clientInfo.name and clientInfo.version must be non-empty strings");

        InitializeRequest initializeRequest;

        try {
            initializeRequest = JsonUtils.treeToValue(jsonNode, InitializeRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("JsonNode converts to bean.", e);
            throw new JsonRpcErrorException(id, JsonRpcErrorCode.INVALID_PARAMS,
                    "Invalid initialize parameters", e);
        }

        InitializeRequestParams requestParams = initializeRequest.getParams();
        List<String> protocolVersions = serverConfig.getProtocolVersions();
        /*
            The server MUST respond with the highest protocol version it supports
            if it does not support the requested (e.g. Client) version.
         */
        if (protocolVersions == null || protocolVersions.isEmpty())
            throw new IllegalStateException("At least one MCP protocol version must be configured");
        for (String configuredVersion : protocolVersions) {
            if (!ProtocolVersion.isSupported(configuredVersion))
                throw new IllegalStateException("SDK does not implement configured protocol version: " + configuredVersion);
        }

        // Configuration is preference ordered; the first entry is the fallback.
        String serverProtocolVersion = protocolVersions.get(0);

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

        InitializeResponseResult.Capabilities capabilities = new InitializeResponseResult.Capabilities();
        // Capabilities are promises. Only advertise methods that this server can
        // actually route; listChanged remains false until mutation APIs emit it.
        if (!featureMgr.getToolStore().isEmpty()) {
            InitializeResponseResult.Capabilities.Tools tools = new InitializeResponseResult.Capabilities.Tools();
            tools.setListChanged(true);
            capabilities.setTools(tools);
        }

        if (!featureMgr.getPromptStore().isEmpty()) {
            InitializeResponseResult.Capabilities.Prompts prompts = new InitializeResponseResult.Capabilities.Prompts();
            prompts.setListChanged(true);
            capabilities.setPrompts(prompts);
        }

        if (!featureMgr.getResourceStore().isEmpty() || !featureMgr.getResourceTemplateStore().isEmpty()) {
            InitializeResponseResult.Capabilities.Resources resources = new InitializeResponseResult.Capabilities.Resources();
            resources.setListChanged(true);
            resources.setSubscribe(true);
            capabilities.setResources(resources);
        }

        capabilities.setLogging(new InitializeResponseResult.Capabilities.Logging());

        if (!featureMgr.getCompletionStore().isEmpty())
            capabilities.setCompletions(new InitializeResponseResult.Capabilities.Completions());

        InitializeResponseResult result = new InitializeResponseResult();
        result.setProtocolVersion(serverProtocolVersion);
        result.setServerInfo(serverInfo);
        result.setCapabilities(capabilities);
        onProtocolNegotiated(serverProtocolVersion, requestParams);
        InitializeResponse resp = new InitializeResponse();
        resp.setId(id);
        resp.setResult(result);

        return resp;
    }

    /**
     * Executes the non empty text operation.
     *
     * @param node the node value.
     * @return the result of the non empty text operation.
     */
    private static boolean nonEmptyText(JsonNode node) {
        return node != null && node.isTextual() && McpUtils.hasText(node.textValue());
    }

    /**
     * Hook used by the concrete server to bind negotiated state to its transport session.
     *
     * @param version       the negotiated protocol version.
     * @param requestParams the client's initialization parameters.
     */
    protected void onProtocolNegotiated(String version, InitializeRequestParams requestParams) {
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
            jsonNode = JsonUtils.readTree(inputJson);
        } catch (IOException e) {
            throw new JsonRpcErrorException(JsonRpcErrorCode.PARSE_ERROR, "Unable to parse the JSON message");
        }

        JsonNode jsonrpcNode = jsonNode.get("jsonrpc");

        if (jsonrpcNode == null)
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Empty jsonrpc version.");

        if (!jsonrpcNode.isTextual())
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "jsonrpc version must be a string.");

        String jsonrpc = jsonrpcNode.asText();

        if (McpUtils.isEmptyText(jsonrpc) || !BaseJsonRpcMessage.VERSION.equals(jsonrpc))
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Invalid jsonrpc version: " + jsonrpc);

        // id 必填
        JsonNode idNode = jsonNode.get(ID);
        Object id = null;

        if (idNode != null) {
            if (idNode.isIntegralNumber())
                id = idNode.longValue();
            else if (idNode.isTextual())
                id = idNode.textValue();
            else
                throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST,
                        "JSON-RPC id must be a string or integer");
        }

        JsonNode methodNode = jsonNode.get(METHOD);

        if (methodNode == null || !methodNode.isTextual())
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Method must be a string.");

        String method = methodNode.asText();

        if (McpUtils.isEmptyText(method))
            throw new JsonRpcErrorException(JsonRpcErrorCode.INVALID_REQUEST, "Method must not be empty.");

        return new McpRequestRawInfo(id, method, jsonNode);
    }
}
