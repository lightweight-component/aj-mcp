package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.common.Content;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.resource.GetResourceListRequest;
import com.ajaxjs.mcp.protocol.tools.*;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.protocol.utils.ping.PingResponse;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP Server Tools
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServer extends McpServerPrompt {
    public void start() {
        log.info("MCP Server started, waiting for input...");
        transport.start();
    }

    /**
     * Handles received message requests.
     * Invokes the corresponding processing logic based on the method.
     *
     * @param requestRaw The raw request information, including the method, ID, and data
     * @return A response object depending on the requested method
     * @throws JsonRpcErrorException If the requested method is not found, this exception is thrown
     */
    public McpResponse processMessage(McpRequestRawInfo requestRaw) {
        switch (requestRaw.getMethod()) {
            case Methods.INITIALIZE:
                JsonNode jsonNode = requestRaw.getJsonNode();
                return initialize(requestRaw.getId(), jsonNode);
            case Methods.PING:
                PingResponse resp = new PingResponse();
                resp.setId(requestRaw.getId());

                return resp;
            case Methods.PROMPTS_LIST:
                return promptList(requestRaw);
            case Methods.PROMPTS_GET:
                return promptGet(requestRaw);
            case Methods.RESOURCES_LIST:
                return resourceList(requestRaw);
            case Methods.RESOURCES_READ:
                return resourceRead(requestRaw);
            case Methods.TOOLS_LIST:
                return toolList(requestRaw);
            case Methods.TOOLS_CALL:
                return toolCall(requestRaw);
            default:
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method " + requestRaw.getMethod() + " not found.");
        }
    }

    /**
     * Processes the tool list request from the client and returns the list of tools.
     * This method first checks if the request contains parameters, and if so, sets them.
     * Then it retrieves the list of tools from the resource store and constructs the response containing the list of tools.
     *
     * @param requestRaw The raw information of the client's request, containing the request ID and possibly parameters.
     * @return Returns the response object containing the list of tools.
     * @throws NullPointerException If the resource store is not initialized.
     */
    McpResponse toolList(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        GetResourceListRequest request = new GetResourceListRequest();
        request.setId(requestRaw.getId());

        if (jsonNode.has(PARAMS))
            request.setParams(JsonUtils.jsonNode2bean(jsonNode.get(PARAMS), Cursor.class));

        // get info from RAM
        if (FeatureMgr.TOOL_STORE.isEmpty())
            throw new NullPointerException("Store is NOT initialized.");

        List<ToolItem> tools = new ArrayList<>();

        for (String name : FeatureMgr.TOOL_STORE.keySet()) {
            ServerStoreTool store = getStore(FeatureMgr.TOOL_STORE, name);
            tools.add(store.getTool());
        }

        GetToolListResult result = new GetToolListResult();
        result.setId(requestRaw.getId());
        result.setResult(new GetToolListResult.ToolList(tools));

        return result;
    }

    /**
     * Calls a tool using the provided request information.
     * <p>
     * This method processes the request to call a tool by parsing the request parameters,
     * validating them against the tool's input schema, and then invoking the tool's method
     * with the provided arguments. It handles errors such as invalid parameters and
     * runtime exceptions during method invocation.
     *
     * @param requestRaw The raw information of the tool call request, containing the necessary
     *                   information to make the call, such as parameters and request ID.
     * @return Returns a response object containing the result of the tool call.
     * @throws JsonRpcErrorException If the parameters are invalid or missing required arguments.
     * @throws RuntimeException      If an exception occurs during method invocation.
     */
    McpResponse toolCall(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        JsonNode paramsNode = jsonNode.get(PARAMS);

        if (paramsNode == null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "params is required");

        CallToolRequest.Params params = JsonUtils.jsonNode2bean(paramsNode, CallToolRequest.Params.class);
        Map<String, Object> arguments = params.getArguments();

        ServerStoreTool store = getStore(FeatureMgr.TOOL_STORE, params.getName());
        ToolItem tool = store.getTool();
        JsonSchema inputSchema = tool.getInputSchema();

        Object[] argValues = null;

        if (inputSchema != null) {
            if (arguments == null || arguments.isEmpty())
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments is required!");

            List<String> required = inputSchema.getRequired();
            if (arguments.size() < required.size())
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments size is not match!");

            Map<String, JsonSchemaProperty> argumentsDefined = inputSchema.getProperties();
            List<String> paramsOrder = store.getParamsOrder();
            argValues = new Object[paramsOrder.size()];

            if (argumentsDefined != null && !argumentsDefined.isEmpty()) {
                for (int i = 0; i < paramsOrder.size(); i++) {
                    String name = paramsOrder.get(i);

                    Object arg = arguments.get(name);

                    if (arg == null && required.contains(name))
                        throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments " + name + " is required!");

                    JsonSchemaProperty property = argumentsDefined.get(name);
                    String type = property.getType();

                    argValues[i] = convertToType(arg, type);
                }
            }
        }

        // executes tool method
        Method method = store.getMethod();
        Object returnedValue;

        try {
            if (argValues == null)
                returnedValue = method.invoke(store.getInstance());
            else
                returnedValue = method.invoke(store.getInstance(), argValues);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        List<Content> content;

        if (returnedValue instanceof Content)
            content = Collections.singletonList((Content) returnedValue);
        else if (returnedValue instanceof String)
            content = Collections.singletonList(new ContentText((String) returnedValue));
        else if (returnedValue instanceof List)
            content = (List<Content>) returnedValue;
        else
            content = Collections.singletonList(new ContentText(returnedValue.toString()));

        CallToolResult.CallToolResultDetail detail = new CallToolResult.CallToolResultDetail();
        detail.setContent(content);

        CallToolResult result = new CallToolResult();
        result.setId(requestRaw.getId());
        result.setResult(detail);

        return result;
    }

    /**
     * Converts the given value to the target type.
     *
     * @param value      The value to be converted, must not be null
     * @param targetType The target type, supported types include: "string", "number", "boolean"
     * @return The converted value
     * @throws IllegalArgumentException if value or targetType is null, or if the conversion is not possible
     */
    public static Object convertToType(Object value, String targetType) {
        if (value == null || targetType == null)
            throw new IllegalArgumentException("Value or targetType cannot be null");

        switch (targetType.toLowerCase()) {
            case "string":
                return value.toString();
            case "number":
                // 首先检查是否是数字类型
                if (value instanceof Number) {
                    Number numberValue = (Number) value;

                    if (numberValue.doubleValue() == numberValue.intValue()) {
                        // 如果没有小数部分，判断为整数
                        if (numberValue.longValue() == numberValue.intValue()) {
                            // 如果值在 int 范围内，返回 int
                            return numberValue.intValue();
                        } else
                            // 否则返回 long
                            return numberValue.longValue();
                    } else
                        // 如果有小数部分，返回 double
                        return numberValue.doubleValue();
                }

                // 如果是字符串值，尝试解析成数字
                try {
                    String strValue = value.toString();
                    if (strValue.contains("."))  // 判断为小数，返回 double
                        return Double.parseDouble(strValue);
                    else {
                        // 判断为整数
                        long longValue = Long.parseLong(strValue);

                        if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE)
                            return (int) longValue; // 如果值在 int 范围内，返回 int
                        else
                            return longValue;  // 否则返回 long
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Value cannot be converted to Number: " + value);
                }
            case "boolean":
                if (value instanceof Boolean)
                    return value; // 如果已经是布尔值，直接返回

                String strValue = value.toString().toLowerCase();
                if ("true".equals(strValue) || "false".equals(strValue))
                    return Boolean.parseBoolean(strValue);

                throw new IllegalArgumentException("Value cannot be converted to Boolean: " + value);

            default:
                throw new IllegalArgumentException("Unsupported targetType: " + targetType);
        }
    }

}