package com.ajaxjs.mcp.client;


import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.GetToolListRequest;
import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.ajaxjs.mcp.protocol.utils.CancellationNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * MCP Client
 * TODO: currently we request a new list of tools every time,
 * so we should add support for the `ToolListChangedNotification` message, and then we can cache the list
 */
@Slf4j
public class McpClient extends McpClientResource {
    public McpClient(Builder builder) {
        super(builder);
        initialize();
    }

    @Override
    public List<ToolItem> listTools() {
        GetToolListRequest request = new GetToolListRequest();
        request.setId(idGenerator.getAndIncrement());
        JsonNode result;

        try {
            result = transport.sendRequestWithResponse(request).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(request.getId());
        }

        return toolListFromMcpResponse((ArrayNode) result.get(RESPONSE_RESULT).get("tools"));
    }

    /**
     * Converts the 'tools' element from a ListToolsResult MCP message to a list of ToolSpecification objects.
     */
    public static List<ToolItem> toolListFromMcpResponse(ArrayNode array) {
        List<ToolItem> result = new ArrayList<>(array.size());

        for (JsonNode tool : array) {
            ToolItem toolSpecification = new ToolItem();
            toolSpecification.setName(tool.get("name").asText());

            if (tool.has("description"))
                toolSpecification.setDescription(tool.get("description").asText());

            JsonNode jsonNode = tool.get("inputSchema");
            JsonSchema jsonSchema = JsonUtils.jsonNode2bean(jsonNode, JsonSchema.class);
            toolSpecification.setInputSchema(jsonSchema);

            result.add(toolSpecification);
        }

        return result;
    }

    @Override
    public String callTool(CallToolRequest request) {
        long operationId = idGenerator.getAndIncrement();
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();

        request.setId(operationId);
        JsonNode result;

        try {
            CompletableFuture<JsonNode> resultFuture = transport.sendRequestWithResponse(request);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            transport.sendRequestWithoutResponse(new CancellationNotification(String.valueOf(operationId), "Timeout"));
            ObjectNode resultTimeout = JsonNodeFactory.instance.objectNode();
            resultTimeout.putObject(RESPONSE_RESULT)
                    .putArray("content").addObject().put("type", ContentType.TEXT)
                    .put(ContentType.TEXT, "There was a timeout executing the tool");

            return extractResult(resultTimeout);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(operationId);
        }

        return extractResult(result);
    }

    @Override
    public String callTool(String name, String arguments) {
        return callTool(new CallToolRequest(name, arguments));
    }

    private static final String EXECUTION_ERROR_MESSAGE = "There was an error executing the tool";

    /**
     * Extracts a response from a CallToolResult message. This may be an error response.
     */
    public static String extractResult(JsonNode result) {
        if (result.has(RESPONSE_RESULT)) {
            JsonNode resultNode = result.get(RESPONSE_RESULT);

            if (resultNode.has("content")) {
                String content = extractSuccessfulResult((ArrayNode) resultNode.get("content"));
                boolean isError = false;

                if (resultNode.has("isError"))
                    isError = resultNode.get("isError").asBoolean();

                if (isError)
                    content = String.format(EXECUTION_ERROR_MESSAGE + ". The tool returned: %s", content);

                return content;
            } else {
                log.warn("Result does not contain 'content' element: {}", result);
                return EXECUTION_ERROR_MESSAGE;
            }
        } else {
            if (result.has("error"))
                return extractError(result.get("error"));

            log.warn("Result contains neither 'result' nor 'error' element: {}", result);
            return EXECUTION_ERROR_MESSAGE;
        }
    }

    private static String extractSuccessfulResult(ArrayNode contents) {
        Stream<JsonNode> contentStream = StreamSupport.stream(contents.spliterator(), false);

        return contentStream.map(content -> {
            if (!content.get("type").asText().equals(ContentType.TEXT))
                throw new RuntimeException("Unsupported content type: " + content.get("type"));

            return content.get(ContentType.TEXT).asText();
        }).collect(Collectors.joining("\n"));
    }

    private static String extractError(JsonNode errorNode) {
        String errorMessage = "";
        if (errorNode.get("message") != null)
            errorMessage = errorNode.get("message").asText("");

        Integer errorCode = null;
        if (errorNode.get("code") != null)
            errorCode = errorNode.get("code").asInt();

        log.warn("Result contains an error: {}, code: {}", errorMessage, errorCode);

        return String.format(EXECUTION_ERROR_MESSAGE + ". Message: %s. Code: %s", errorMessage, errorCode);
    }
}
