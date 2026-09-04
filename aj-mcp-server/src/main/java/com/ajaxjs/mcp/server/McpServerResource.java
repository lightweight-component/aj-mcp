package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.resource.*;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.server.common.PaginatedResponse;
import com.ajaxjs.mcp.server.common.ServerUtils;
import com.ajaxjs.mcp.server.error.JsonRpcErrorCode;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResource;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResourceTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Represents mcp server resource.
 */
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

        List<ResourceItem> resources = new ArrayList<>();

        for (ServerStoreResource store : featureMgr.getResourceStore().values()) {
            ResourceItem resourceItem = store.getResource();
            resources.add(resourceItem);
        }

        GetResourceListResultDetail resultList;

        if (request.getParams() != null && request.getParams().getPageNo() != null) {
            // do the page
            PaginatedResponse<ResourceItem> page = ServerUtils.paginate(resources, request.getParams(), this);
            resources = page.getList();
            resultList = new GetResourceListResultDetail(resources);

            if (!page.isLastPage())
                resultList.setNextCursor(page.getNextPageNoAsBse64());
        } else
            resultList = new GetResourceListResultDetail(resources);

        GetResourceListResult result = new GetResourceListResult(resultList);
        result.setId(requestRaw.getId());

        return result;
    }

    /**
     * Executes the resource template list operation.
     *
     * @param requestRaw the request raw value.
     * @return the result of the resource template list operation.
     */
    McpResponse resourceTemplateList(McpRequestRawInfo requestRaw) {
        Cursor cursor = requestRaw.getJsonNode().has(PARAMS) ? JsonUtils.jsonNode2bean(requestRaw.getJsonNode().get(PARAMS), Cursor.class) : null;
        List<ResourceTemplate> templates = new ArrayList<>();

        for (ServerStoreResourceTemplate store : featureMgr.getResourceTemplateStore().values())
            templates.add(store.getResourceTemplate());

        ResourceTemplatesResultDetail detail = new ResourceTemplatesResultDetail();

        if (cursor != null && cursor.getPageNo() != null) {
            PaginatedResponse<com.ajaxjs.mcp.protocol.resource.ResourceTemplate> page = ServerUtils.paginate(templates, cursor, this);
            detail.setResourceTemplates(page.getList());

            if (!page.isLastPage())
                detail.setNextCursor(page.getNextPageNoAsBse64());
        } else
            detail.setResourceTemplates(templates);

        ResourceTemplateResult response = new ResourceTemplateResult(detail);
        response.setId(requestRaw.getId());

        return response;
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

        GetResourceRequestParams params = JsonUtils.jsonNode2bean(paramsNode, GetResourceRequestParams.class);
        ServerStoreResource store = featureMgr.getResourceStore().get(params.getUri());

        if (store == null)
            return readTemplateResource(requestRaw.getId(), params.getUri());

        // execute prompt method
        Method method = store.getMethod();
        Object returnedValue;

        try {
            returnedValue = method.invoke(store.getInstance());
        } catch (IllegalAccessException e) {
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INTERNAL_ERROR,
                    "Resource method is not accessible: " + params.getUri(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new JsonRpcErrorException(requestRaw.getId(), JsonRpcErrorCode.INTERNAL_ERROR,
                    "Resource execution failed: " + errorMessage(cause), cause);
        }

        List<ResourceContent> contents = resourceContents(requestRaw.getId(), returnedValue, "Resource");

        GetResourceResult result = new GetResourceResult();
        result.setId(requestRaw.getId());
        result.setResult(new GetResourceResultDetail(contents));

        return result;
    }

    /**
     * Executes the read template resource operation.
     *
     * @param requestId the request id value.
     * @param uri       the uri value.
     * @return the result of the read template resource operation.
     */
    private McpResponse readTemplateResource(Object requestId, String uri) {
        for (ServerStoreResourceTemplate store : featureMgr.getResourceTemplateStore().values()) {
            Matcher matcher = store.getUriPattern().matcher(uri);

            if (!matcher.matches())
                continue;

            Map<String, String> captured = new LinkedHashMap<>();

            for (int i = 0; i < store.getTemplateVariableNames().size(); i++)
                captured.put(store.getTemplateVariableNames().get(i), decodeUriPart(matcher.group(i + 1)));

            Object[] values = new Object[store.getParameterNames().size()];
            Class<?>[] types = store.getMethod().getParameterTypes();

            for (int i = 0; i < values.length; i++)
                values[i] = McpServer.convertToType(captured.get(store.getParameterNames().get(i)), types[i]);

            Object returned;

            try {
                returned = store.getMethod().invoke(store.getInstance(), values);
            } catch (IllegalAccessException e) {
                throw new JsonRpcErrorException(requestId, JsonRpcErrorCode.INTERNAL_ERROR,
                        "Resource template method is not accessible", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw new JsonRpcErrorException(requestId, JsonRpcErrorCode.INTERNAL_ERROR,
                        "Resource template execution failed: " + errorMessage(cause), cause);
            }

            List<ResourceContent> contents = resourceContents(requestId, returned, "Resource template");

            GetResourceResult response = new GetResourceResult();
            response.setId(requestId);
            response.setResult(new GetResourceResultDetail(contents));

            return response;
        }
        throw new JsonRpcErrorException(requestId, JsonRpcErrorCode.INVALID_PARAMS, "Unknown resource: " + uri);
    }

    /**
     * Executes the resource contents operation.
     *
     * @param requestId the request id value.
     * @param returned  the returned value.
     * @param feature   the feature value.
     * @return the result of the resource contents operation.
     */
    private static List<ResourceContent> resourceContents(Object requestId, Object returned, String feature) {
        if (returned instanceof ResourceContent)
            return Collections.singletonList((ResourceContent) returned);

        if (returned instanceof List) {
            List<ResourceContent> contents = new ArrayList<>();
            for (Object value : (List<?>) returned) {
                if (!(value instanceof ResourceContent))
                    throw invalidResourceReturn(requestId, feature, value);
                contents.add((ResourceContent) value);
            }
            return contents;
        }

        throw invalidResourceReturn(requestId, feature, returned);
    }

    /**
     * Executes the invalid resource return operation.
     *
     * @param requestId the request id value.
     * @param feature   the feature value.
     * @param value     the value value.
     * @return the result of the invalid resource return operation.
     */
    private static JsonRpcErrorException invalidResourceReturn(Object requestId, String feature, Object value) {
        return new JsonRpcErrorException(requestId, JsonRpcErrorCode.INTERNAL_ERROR,
                feature + " returned an unsupported value: "
                        + (value == null ? "null" : value.getClass().getName()));
    }

    /**
     * Executes the decode uri part operation.
     *
     * @param value the value value.
     * @return the result of the decode uri part operation.
     */
    private static String decodeUriPart(String value) {
        try {
            // URLDecoder treats '+' as a space; preserve it because URI paths do not.
            return URLDecoder.decode(value.replace("+", "%2B"), "UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Executes the error message operation.
     *
     * @param cause the cause value.
     * @return the result of the error message operation.
     */
    private static String errorMessage(Throwable cause) {
        String message = cause.getMessage();
        return message == null || message.trim().isEmpty() ? cause.getClass().getSimpleName() : message;
    }
}
