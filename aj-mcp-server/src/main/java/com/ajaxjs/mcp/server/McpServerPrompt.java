package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.prompt.*;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.model.ServerStorePrompt;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class McpServerPrompt extends McpServerResource {
    McpResponse promptList(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        GetPromptListRequest request = new GetPromptListRequest();
        request.setId(requestRaw.getId());

        if (jsonNode.has(PARAMS))
            request.setParams(JsonUtils.jsonNode2bean(jsonNode.get(PARAMS), Cursor.class));

        // get info from RAM
        if (FeatureMgr.PROMPT_STORE.isEmpty())
            throw new NullPointerException("Store 未初始化");

        List<PromptItem> prompts = new ArrayList<>();
        for (String name : FeatureMgr.PROMPT_STORE.keySet()) {
            ServerStorePrompt store = getStore(FeatureMgr.PROMPT_STORE, name);
            PromptItem promptItem = store.getPrompt();
            prompts.add(promptItem);
        }

        GetPromptListResult.PromptResult resultList = new GetPromptListResult.PromptResult(prompts);
        GetPromptListResult result = new GetPromptListResult(resultList);
        result.setId(requestRaw.getId());

        return result;
    }

    McpResponse promptGet(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        JsonNode paramsNode = jsonNode.get(PARAMS);

        if (paramsNode == null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "params is required");

        GetPromptRequest.Params params = JsonUtils.jsonNode2bean(paramsNode, GetPromptRequest.Params.class);

        GetPromptRequest request = new GetPromptRequest(); // seems to be useless
        request.setId(requestRaw.getId());
        request.setParams(params);

        ServerStorePrompt store = getStore(FeatureMgr.PROMPT_STORE, params.getName());
        PromptItem prompt = store.getPrompt();

        Object[] argValues = null;
        List<PromptArgument> argumentsDefined = prompt.getArguments();

        if (argumentsDefined != null && !argumentsDefined.isEmpty()) {
            Map<String, Object> arguments = params.getArguments();

            if (arguments == null || arguments.isEmpty())
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments is required!");

            if (argumentsDefined.size() != arguments.size())
                throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "arguments size is not match!");

            /*
            * Java Map 结构如 JSON 所示 {
                  "code": "def hello():\n    print('world')",
            "abc":1
                }
            其中 key 对应 java 普通方法的参数列表中的参数名，但顺序不一定。参数列表如 ["abc", "code"]
            如何把 map 的 value 按照参数顺序 转换为 object[]
             */
            String[] paramOrder = new String[argumentsDefined.size()];
            for (int i = 0; i < argumentsDefined.size(); i++)
                paramOrder[i] = argumentsDefined.get(i).getName();

            argValues = extractValues(arguments, paramOrder);
        }

        // execute prompt method
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
            throw new RuntimeException(e);
        }

        List<PromptMessage> promptMessages = null;

        if (returnedValue instanceof PromptMessage)
            promptMessages = Collections.singletonList((PromptMessage) returnedValue);
         else if (returnedValue instanceof List)
            promptMessages = (List<PromptMessage>) returnedValue;

        GetPromptResult.PromptResultDetail promptResultDetail = new GetPromptResult.PromptResultDetail();
        promptResultDetail.setDescription(prompt.getDescription());
        promptResultDetail.setMessages(promptMessages);

        GetPromptResult result = new GetPromptResult();
        result.setId(requestRaw.getId());
        result.setResult(promptResultDetail);

        return result;
    }

    static Object[] extractValues(Map<String, Object> map, String[] paramOrder) {
        // 创建一个 Object 数组用于存储结果
        Object[] result = new Object[paramOrder.length];

        // 按照参数列表的顺序提取值
        for (int i = 0; i < paramOrder.length; i++) {
            String key = paramOrder[i];

            if (map.containsKey(key)) {
                result[i] = map.get(key);
            } else {
                // 如果 Map 中不存在对应的 key，可以选择设置默认值或抛出异常
                result[i] = null; // 默认值为 null
            }
        }

        return result;
    }
}
