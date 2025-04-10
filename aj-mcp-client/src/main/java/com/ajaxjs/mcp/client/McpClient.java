package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.protocol.CancellationNotification;
import com.ajaxjs.mcp.client.protocol.tool.CallToolRequest;
import com.ajaxjs.mcp.client.protocol.tool.ListToolsRequest;
import com.ajaxjs.mcp.client.tool.ToolSpecification;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.json.*;
import com.ajaxjs.mcp.message.ToolExecutionRequest;
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
 * This class impl
 * TODO: currently we request a new list of tools every time, so we should add support for the `ToolListChangedNotification` message, and then we can cache the list
 */
@Slf4j
public class McpClient extends ClientResource {
    public McpClient(Builder builder) {
        super(builder);
        initialize();
    }

    @Override
    public List<ToolSpecification> listTools() {
        ListToolsRequest operation = new ListToolsRequest(idGenerator.getAndIncrement());
        CompletableFuture<JsonNode> resultFuture = transport.executeOperationWithResponse(operation);
        JsonNode result;

        try {
            result = resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }

        return toolSpecificationListFromMcpResponse((ArrayNode) result.get("result").get("tools"));
    }

    /**
     * Converts the 'tools' element from a ListToolsResult MCP message to a list of ToolSpecification objects.
     */
    static List<ToolSpecification> toolSpecificationListFromMcpResponse(ArrayNode array) {
        List<ToolSpecification> result = new ArrayList<>(array.size());

        for (JsonNode tool : array) {
            ToolSpecification toolSpecification = new ToolSpecification();
            toolSpecification.setName(tool.get("name").asText());

            if (tool.has("description"))
                toolSpecification.setDescription(tool.get("description").asText());

            toolSpecification.setParameters((JsonObjectSchema) jsonNodeToJsonSchemaElement(tool.get("inputSchema")));
            result.add(toolSpecification);
        }

        return result;
    }

    @Override
    public String executeTool(ToolExecutionRequest executionRequest) {
        ObjectNode arguments = executionRequest.getArguments() == null ? null : JsonUtils.fromJson(executionRequest.getArguments(), ObjectNode.class);
        long operationId = idGenerator.getAndIncrement();
        CallToolRequest operation = new CallToolRequest(operationId, executionRequest.getName(), arguments);
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();
        CompletableFuture<JsonNode> resultFuture;
        JsonNode result;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeout) {
            transport.executeOperationWithoutResponse(new CancellationNotification(operationId, "Timeout"));

            ObjectNode resultTimeout = JsonNodeFactory.instance.objectNode();
            resultTimeout.putObject("result")
                    .putArray("content")
                    .addObject()
                    .put("type", "text")
                    .put("text", "There was a timeout executing the tool");

            return extractResult(resultTimeout);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }

        return extractResult(result);
    }

    /**
     * Converts the 'inputSchema' element (inside the 'Tool' type in the MCP schema)
     * to a JsonSchemaElement object that describes the tool's arguments.
     */
    static JsonSchemaElement jsonNodeToJsonSchemaElement(JsonNode node) {
        String nodeType = node.get("type").asText();

        switch (nodeType) {
            case "object": {
                JsonObjectSchema.JsonObjectSchemaBuilder builder = JsonObjectSchema.builder();
                JsonNode required = node.get("required");
//            if (required != null)
//                builder.required(toStringArray((ArrayNode) required));

                if (node.has("additionalProperties"))
                    builder.additionalProperties(node.get("additionalProperties").asBoolean(false));

                JsonNode description = node.get("description");
                if (description != null)
                    builder.description(description.asText());

                JsonNode properties = node.get("properties");
                if (properties != null) {
                    ObjectNode propertiesObject = (ObjectNode) properties;
//                for (Map.Entry<String, JsonNode> property : propertiesObject.properties())
//                    builder.addProperty(property.getKey(), jsonNodeToJsonSchemaElement(property.getValue()));
                }

                return builder.build();
            }
            case "string":
                if (node.has("enum")) {
                    JsonEnumSchema.JsonEnumSchemaBuilder builder = JsonEnumSchema.builder();
                    if (node.has("description"))
                        builder.description(node.get("description").asText());

//                builder.enumValues(toStringArray((ArrayNode) node.get("enum")));
                    return builder.build();
                } else {
                    JsonStringSchema.JsonStringSchemaBuilder builder = JsonStringSchema.builder();
                    if (node.has("description"))
                        builder.description(node.get("description").asText());

                    return builder.build();
                }
            case "number": {
                JsonNumberSchema.JsonNumberSchemaBuilder builder = JsonNumberSchema.builder();
                if (node.has("description"))
                    builder.description(node.get("description").asText());

                return builder.build();
            }
            case "integer": {
                JsonIntegerSchema.JsonIntegerSchemaBuilder builder = JsonIntegerSchema.builder();
                if (node.has("description"))
                    builder.description(node.get("description").asText());

                return builder.build();
            }
            case "boolean": {
                JsonBooleanSchema.JsonBooleanSchemaBuilder builder = JsonBooleanSchema.builder();
                if (node.has("description"))
                    builder.description(node.get("description").asText());

                return builder.build();
            }
            case "array": {
                JsonArraySchema.JsonArraySchemaBuilder builder = JsonArraySchema.builder();
                if (node.has("description"))
                    builder.description(node.get("description").asText());

                builder.items(jsonNodeToJsonSchemaElement(node.get("items")));
                return builder.build();
            }
            default:
                throw new IllegalArgumentException("Unknown element type: " + nodeType);
        }
    }

    private static String[] toStringArray(ArrayNode jsonArray) {
        String[] result = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++)
            result[i] = jsonArray.get(i).asText();

        return result;
    }

    private static final String EXECUTION_ERROR_MESSAGE = "There was an error executing the tool";

    /**
     * Extracts a response from a CallToolResult message. This may be an error response.
     */
    public static String extractResult(JsonNode result) {
        if (result.has("result")) {
            JsonNode resultNode = result.get("result");

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
            if (!content.get("type").asText().equals("text"))
                throw new RuntimeException("Unsupported content type: " + content.get("type"));

            return content.get("text").asText();
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
