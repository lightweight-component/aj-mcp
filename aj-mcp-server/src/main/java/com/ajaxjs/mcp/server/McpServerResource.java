package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.prompt.GetPromptListResult;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.resource.*;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.server.common.PaginatedResponse;
import com.ajaxjs.mcp.server.common.ServerUtils;
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
    /**
     * Retrieves a list of resources based on the request information.
     * This method first checks if the request contains parameters, and if so, sets these parameters.
     * It then obtains resource information from the RAM (Resource Access Management) system.
     * If the resource store has not been initialized, it throws a NullPointerException.
     * Finally, it constructs and returns the resource list result.
     *
     * @param requestRaw The raw information of the request, containing the necessary information to retrieve the resource list.
     * @return Returns the resource list response object, containing the list of resources.
     * @throws NullPointerException If the resource store has not been initialized, this exception is thrown.
     */
    McpResponse resourceList(McpRequestRawInfo requestRaw) {
        JsonNode jsonNode = requestRaw.getJsonNode();
        GetResourceListRequest request = new GetResourceListRequest();
        request.setId(requestRaw.getId());

        if (jsonNode.has(PARAMS))
            request.setParams(JsonUtils.jsonNode2bean(jsonNode.get(PARAMS), Cursor.class));

        // get info from RAM
        if (FeatureMgr.RESOURCE_STORE.isEmpty())
            throw new NullPointerException("Store is NOT initialized");

        List<ResourceItem> resources = new ArrayList<>();
        for (String name : FeatureMgr.RESOURCE_STORE.keySet()) {
            ServerStoreResource store = getStore(FeatureMgr.RESOURCE_STORE, name);
            ResourceItem resourceItem = store.getResource();
            resources.add(resourceItem);
        }

        GetResourceListResult.ResourceResult resultList;

        if (request.getParams() != null && request.getParams().getPageNo() != null) {
            // do the page
            PaginatedResponse<ResourceItem> page = ServerUtils.paginate(resources, request.getParams(), this);
            resources = page.getList();
            resultList = new GetResourceListResult.ResourceResult(resources);

            if (!page.isLastPage())
                resultList.setNextCursor(page.getNextPageNoAsBse64());
        } else
            resultList = new GetResourceListResult.ResourceResult(resources);

        GetResourceListResult result = new GetResourceListResult(resultList);
        result.setId(requestRaw.getId());

        return result;
    }

    /**
     * Reads resource information based on the request.
     * This method interprets the request parameters, retrieves the corresponding resource from the server store,
     * and invokes the resource's method to obtain the resource content. It then packages and returns the result.
     *
     * @param requestRaw The raw information of the resource read request, containing the request ID and parameters.
     * @return Returns the read resource information, including the request ID and resource content.
     * @throws JsonRpcErrorException If the request parameters are invalid, throws a custom JSON RPC error exception.
     * @throws RuntimeException      If there is an exception invoking the resource method, throws a runtime exception.
     */
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
}
