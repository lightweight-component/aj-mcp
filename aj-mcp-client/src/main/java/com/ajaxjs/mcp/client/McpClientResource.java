package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.resource.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@SuperBuilder
public abstract class McpClientResource extends McpClientPrompt {
    @Override
    public List<ResourceItem> listResources() {
        if (resourceRefs.get() == null)
            obtainResourceList();

        return resourceRefs.get();
    }

    @Override
    public GetResourceResult.ResourceResultDetail readResource(String uri) {
        long operationId = idGenerator.getAndIncrement();
        GetResourceRequest request = new GetResourceRequest();
        request.setId(operationId);
        request.setParams(new GetResourceRequest.Params(uri));

        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();

        try {
            CompletableFuture<JsonNode> resultFuture = transport.sendRequestWithResponse(request);
            JsonNode result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);

            return parseResourceContents(result);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(operationId);
        }
    }

    @Override
    public List<ResourceTemplate> listResourceTemplates() {
        if (resourceTemplateRefs.get() == null)
            obtainResourceTemplateList();

        return resourceTemplateRefs.get();
    }

    /**
     * Synchronously obtains the list of resources.
     * This method ensures that the resource references are initialized only once by checking if they already exist.
     * If the resource references are already available, it returns immediately without making a new request.
     * <p>
     * A new request is created with a unique ID to fetch the resource list. It uses a timeout mechanism to handle long waits.
     * The method sends the request and waits for the response using CompletableFuture.
     * Once the result is received, it parses the response and sets the parsed resource references.
     * <p>
     * If an error occurs during the request (execution exception, interruption, or timeout),
     * a RuntimeException is thrown to propagate the failure.
     * <p>
     * Finally, the pending request ID is removed to clean up internal state, regardless of success or failure.
     *
     * @throws RuntimeException if the request execution, interruption, or timeout occurs
     */
    private synchronized void obtainResourceList() {
        if (resourceRefs.get() != null)
            return;

        GetResourceListRequest request = new GetResourceListRequest();
        request.setId(idGenerator.getAndIncrement());

        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();

        try {
            CompletableFuture<JsonNode> resultFuture = transport.sendRequestWithResponse(request);
            JsonNode result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            resourceRefs.set(parseResourceRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(request.getId());
        }
    }

    private synchronized void obtainResourceTemplateList() {
        if (resourceTemplateRefs.get() != null)
            return;

        GetResourceTemplateListRequest request = new GetResourceTemplateListRequest();
        request.setId(idGenerator.getAndIncrement());
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();

        try {
            CompletableFuture<JsonNode> resultFuture = transport.sendRequestWithResponse(request);
            JsonNode result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            resourceTemplateRefs.set(parseResourceTemplateRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(request.getId());
        }
    }

    /**
     * Parses resource content from an MCP message and constructs a ResourceResultDetail object.
     * This method checks for errors in the provided JsonNode and processes its structure to extract resource content.
     *
     * @param mcpMessage The JSON node representing the MCP message containing resource content.
     * @return A GetResourceResult.ResourceResultDetail object populated with parsed resource content details.
     * @throws McpException          If the MCP message contains error information.
     * @throws IllegalStateException If the message structure is invalid (e.g., missing required fields).
     */
    public static List<ResourceItem> parseResourceRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has(RESPONSE_RESULT)) {
            JsonNode resultNode = mcpMessage.get(RESPONSE_RESULT);

            if (resultNode.has("resources")) {
                List<ResourceItem> resourceRefs = new ArrayList<>();

                for (JsonNode resourceNode : resultNode.get("resources"))
                    resourceRefs.add(JsonUtils.convertValue(resourceNode, ResourceItem.class));

                return resourceRefs;
            } else {
                log.warn("Result does not contain 'resources' element: {}", resultNode);
                throw new IllegalStateException("Result does not contain 'resources' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalStateException("Result does not contain 'result' element");
        }
    }

    public static GetResourceResult.ResourceResultDetail parseResourceContents(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has(RESPONSE_RESULT)) {
            JsonNode resultNode = mcpMessage.get(RESPONSE_RESULT);
            if (resultNode.has("contents")) {
                List<ResourceContent> resourceContentsList = new ArrayList<>();

                for (JsonNode resourceNode : resultNode.get("contents")) {
                    String uri = resourceNode.get("uri").asText();
                    String mimeType = resourceNode.get("mimeType") != null ? resourceNode.get("mimeType").asText() : null;

                    if (resourceNode.has(ContentType.TEXT)) {
                        ResourceContentText content = new ResourceContentText();
                        content.setUri(uri);
                        content.setMimeType(mimeType);
                        content.setText(resourceNode.get(ContentType.TEXT).asText());

                        resourceContentsList.add(content);
                    } else if (resourceNode.has("blob")) {
                        ResourceContentBinary content = new ResourceContentBinary();
                        content.setUri(uri);
                        content.setMimeType(mimeType);
                        content.setBlob(resourceNode.get("blob").asText());

                        resourceContentsList.add(content);
                    }
                }

                return new GetResourceResult.ResourceResultDetail(resourceContentsList);
            } else {
                log.warn("Result does not contain 'contents' element: {}", resultNode);
                throw new IllegalStateException("Result does not contain 'resources' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalStateException("Result does not contain 'result' element");
        }
    }

    public static List<ResourceTemplate> parseResourceTemplateRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has(RESPONSE_RESULT)) {
            JsonNode resultNode = mcpMessage.get(RESPONSE_RESULT);

            if (resultNode.has("resourceTemplates")) {
                List<ResourceTemplate> resourceTemplateRefs = new ArrayList<>();

                for (JsonNode resourceTemplateNode : resultNode.get("resourceTemplates"))
                    resourceTemplateRefs.add(JsonUtils.convertValue(resourceTemplateNode, ResourceTemplate.class));

                return resourceTemplateRefs;
            } else {
                log.warn("Result does not contain 'resourceTemplates' element: {}", resultNode);
                throw new IllegalStateException("Result does not contain 'resourceTemplates' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalStateException("Result does not contain 'result' element");
        }
    }
}
