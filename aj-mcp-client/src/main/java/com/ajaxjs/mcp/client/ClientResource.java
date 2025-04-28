package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.protocol.resource.*;
import com.ajaxjs.mcp.common.IllegalResponseException;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.resource.Resource;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class ClientResource extends ClientPrompt {
    public ClientResource(Builder builder) {
        super(builder);
    }

    @Override
    public List<Resource> listResources() {
        if (resourceRefs.get() == null)
            obtainResourceList();

        return resourceRefs.get();
    }

    @Override
    public ReadResourceResult readResource(String uri) {
        long operationId = idGenerator.getAndIncrement();
        ReadResourceRequest operation = new ReadResourceRequest(operationId, uri);
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return parseResourceContents(result);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    @Override
    public List<ResourceTemplate> listResourceTemplates() {
        if (resourceTemplateRefs.get() == null)
            obtainResourceTemplateList();

        return resourceTemplateRefs.get();
    }

    private synchronized void obtainResourceList() {
        if (resourceRefs.get() != null)
            return;

        ListResourcesRequest operation = new ListResourcesRequest(idGenerator.getAndIncrement());
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            resourceRefs.set(parseResourceRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }
    }

    private synchronized void obtainResourceTemplateList() {
        if (resourceTemplateRefs.get() != null)
            return;

        ListResourceTemplatesRequest operation = new ListResourceTemplatesRequest(idGenerator.getAndIncrement());
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            resourceTemplateRefs.set(parseResourceTemplateRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }
    }

    public static List<Resource> parseResourceRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");

            if (resultNode.has("resources")) {
                List<Resource> resourceRefs = new ArrayList<>();

                for (JsonNode resourceNode : resultNode.get("resources"))
                    resourceRefs.add(JsonUtils.convertValue(resourceNode, Resource.class));

                return resourceRefs;
            } else {
                log.warn("Result does not contain 'resources' element: {}", resultNode);
                throw new IllegalResponseException("Result does not contain 'resources' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }

    public static ReadResourceResult parseResourceContents(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");
            if (resultNode.has("contents")) {
                List<ResourceContents> resourceContentsList = new ArrayList<>();

                for (JsonNode resourceNode : resultNode.get("contents")) {
                    String uri = resourceNode.get("uri").asText();
                    String mimeType = resourceNode.get("mimeType") != null ? resourceNode.get("mimeType").asText() : null;

                    if (resourceNode.has("text")) {
                        resourceContentsList.add(new TextResourceContents(uri, resourceNode.get("text").asText(), mimeType));
                    } else if (resourceNode.has("blob")) {
                        resourceContentsList.add(new BlobResourceContents(uri, resourceNode.get("blob").asText(), mimeType));
                    }
                }

                return new ReadResourceResult(resourceContentsList);
            } else {
                log.warn("Result does not contain 'contents' element: {}", resultNode);
                throw new IllegalResponseException("Result does not contain 'resources' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }

    public static List<ResourceTemplate> parseResourceTemplateRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");

            if (resultNode.has("resourceTemplates")) {
                List<ResourceTemplate> resourceTemplateRefs = new ArrayList<>();

                for (JsonNode resourceTemplateNode : resultNode.get("resourceTemplates"))
                    resourceTemplateRefs.add(JsonUtils.convertValue(resourceTemplateNode, ResourceTemplate.class));

                return resourceTemplateRefs;
            } else {
                log.warn("Result does not contain 'resourceTemplates' element: {}", resultNode);
                throw new IllegalResponseException("Result does not contain 'resourceTemplates' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }
}
