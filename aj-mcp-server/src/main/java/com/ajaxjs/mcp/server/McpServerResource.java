package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.resource.*;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResource;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class McpServerResource extends McpServerInitialize {
    McpResponse resourceList(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        GetResourceListRequest request = new GetResourceListRequest();
        request.setId(requestRaw.getId());

        if (jsonNode.has(PARAMS))
            request.setParams(JsonUtils.jsonNode2bean(jsonNode.get(PARAMS), Cursor.class));

        // get info from RAM
        if (FeatureMgr.RESOURCE_STORE.isEmpty())
            throw new NullPointerException("Store 未初始化");

        List<ResourceItem> resources = new ArrayList<>();
        for (String name : FeatureMgr.RESOURCE_STORE.keySet()) {
            ServerStoreResource store = getStore(FeatureMgr.RESOURCE_STORE, name);
            ResourceItem resourceItem = store.getResource();
            resources.add(resourceItem);
        }

        GetResourceListResult.ResourceResult resultList = new GetResourceListResult.ResourceResult(resources);
        GetResourceListResult result = new GetResourceListResult(resultList);
        result.setId(requestRaw.getId());

        return result;
    }

    McpResponse resourceRead(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        JsonNode paramsNode = jsonNode.get(PARAMS);

        if (paramsNode == null)
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INVALID_PARAMS, "params is required");

        GetResourceRequest.Params params = JsonUtils.jsonNode2bean(paramsNode, GetResourceRequest.Params.class);
        ServerStoreResource store = getStore(FeatureMgr.RESOURCE_STORE, params.getUri());

        // execute prompt method
        Method method = store.getMethod();
        Object returnedValue;

        try {
            returnedValue = method.invoke(store.getInstance());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        List<ResourceContent> contents = Collections.singletonList((ResourceContent) returnedValue);

        GetResourceResult result = new GetResourceResult();
        result.setId(requestRaw.getId());
        result.setResult(new GetResourceResult.ResourceResultDetail(contents));

        return result;
    }

    public static void autoSet(ResourceContent content) {

    }
}
