package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.prompt.GetPromptListRequest;
import com.ajaxjs.mcp.protocol.prompt.GetPromptRequest;
import com.ajaxjs.mcp.protocol.prompt.GetPromptResult;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handing prompts
 */
@Slf4j
@SuperBuilder
public abstract class McpClientPrompt extends McpClientBase {
    @Override
    public List<PromptItem> listPrompts() {
        if (promptRefs.get() == null)
            obtainPromptList();

        return promptRefs.get();
    }

    /**
     * Synchronized method to get the prompt list
     * This method is used to send a request to fetch the prompt list data when the prompt list is empty, and parse and set it into promptRefs.
     * If promptRefs is not empty, it returns directly to avoid redundant fetching.
     * A synchronized mechanism is used to ensure thread-safe access to the prompt list in a multithreading environment.
     */
    private synchronized void obtainPromptList() {
        if (promptRefs.get() != null)
            return;

        GetPromptListRequest request = new GetPromptListRequest();
        request.setId(idGenerator.getAndIncrement());

        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();

        try {
            CompletableFuture<JsonNode> resultFuture = transport.sendRequestWithResponse(request);
            JsonNode result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);

            List<PromptItem> promptItems = parsePromptRefs(result);
            promptRefs.set(promptItems);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(request.getId());
        }
    }

    /**
     * Parses prompt references from the given JSON object and converts them into a list of PromptItem objects.
     *
     * @param mcpMessage The MCP message JSON object containing prompt information
     * @return A list of PromptItem objects representing the parsed prompt references
     * @throws McpException          If the JSON object contains error information, an McpException is thrown
     * @throws IllegalStateException If the structure of the JSON object does not match the expected format, an IllegalStateException is thrown
     */
    private static List<PromptItem> parsePromptRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has(McpConstant.RESPONSE_RESULT)) {
            JsonNode resultNode = mcpMessage.get(McpConstant.RESPONSE_RESULT);

            if (resultNode.has("prompts")) {
                List<PromptItem> promptRefs = new ArrayList<>();
                for (JsonNode promptNode : resultNode.get("prompts"))
                    promptRefs.add(JsonUtils.jsonNode2bean(promptNode, PromptItem.class));

                return promptRefs;
            } else {
                log.warn("Result does not contain 'prompts' element: {}", resultNode);
                throw new IllegalStateException("Result does not contain 'prompts' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalStateException("Result does not contain 'result' element");
        }
    }

    /**
     * Retrieves prompt details by name and arguments.
     * This method constructs a request to get prompt information, sends the request using the transport layer,
     * and returns the parsed result.
     *
     * @param name      The name of the prompt.
     * @param arguments A map containing the arguments for the prompt.
     * @return Returns an object of type {@link GetPromptResult.PromptResultDetail} containing the details of the prompt.
     * @throws RuntimeException If an error occurs during the request sending process or result parsing.
     */
    @Override
    public GetPromptResult.PromptResultDetail getPrompt(String name, Map<String, Object> arguments) {
        long operationId = idGenerator.getAndIncrement();

        GetPromptRequest.Params params = new GetPromptRequest.Params();
        params.setName(name);
        params.setArguments(arguments);

        GetPromptRequest request = new GetPromptRequest(); // seems to be useless
        request.setId(operationId);
        request.setParams(params);

        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();

        try {
            CompletableFuture<JsonNode> resultFuture = transport.sendRequestWithResponse(request);
            JsonNode result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            McpException.checkForErrors(result);

            return JsonUtils.jsonNode2bean(result.get(McpConstant.RESPONSE_RESULT), GetPromptResult.PromptResultDetail.class);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingRequests.remove(operationId);
        }
    }


    /**
     * Retrieves prompt details by name and arguments.
     * This method constructs a request to get prompt information, sends the request using the transport layer,
     * and returns the parsed result.
     *
     * @param name      The name of the prompt.
     * @param arguments A  JSON String containing the arguments for the prompt.
     * @return Returns an object of type {@link GetPromptResult.PromptResultDetail} containing the details of the prompt.
     * @throws RuntimeException If an error occurs during the request sending process or result parsing.
     */
    @Override
    public GetPromptResult.PromptResultDetail getPrompt(String name, String arguments) {
        return getPrompt(name, JsonUtils.json2map(arguments));
    }
}
