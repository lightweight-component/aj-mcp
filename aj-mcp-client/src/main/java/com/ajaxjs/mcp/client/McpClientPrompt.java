package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.common.IllegalResponseException;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpException;
import com.ajaxjs.mcp.protocol.McpConstant;
import com.ajaxjs.mcp.protocol.prompt.GetPromptListRequest;
import com.ajaxjs.mcp.protocol.prompt.GetPromptRequest;
import com.ajaxjs.mcp.protocol.prompt.GetPromptResult;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class McpClientPrompt extends McpClientBase {
    public McpClientPrompt(Builder builder) {
        super(builder);
    }

    @Override
    public List<PromptItem> listPrompts() {
        if (promptRefs.get() == null)
            obtainPromptList();

        return promptRefs.get();
    }

    private synchronized void obtainPromptList() {
        if (promptRefs.get() != null)
            return;

        GetPromptListRequest request = new GetPromptListRequest();
        request.setId(idGenerator.getAndIncrement());

        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();

        try {
            CompletableFuture<JsonNode> resultFuture = transport.executeOperationWithResponse(request);
            JsonNode result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);

            List<PromptItem> promptItems = parsePromptRefs(result);
            promptRefs.set(promptItems);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(request.getId());
        }
    }

    static List<PromptItem> parsePromptRefs(JsonNode mcpMessage) {
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
                throw new IllegalResponseException("Result does not contain 'prompts' element");
            }
        } else {
            log.warn("Result does not contain 'result' element: {}", mcpMessage);
            throw new IllegalResponseException("Result does not contain 'result' element");
        }
    }

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
            CompletableFuture<JsonNode> resultFuture = transport.executeOperationWithResponse(request);
            JsonNode result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            McpException.checkForErrors(result);

            return JsonUtils.jsonNode2bean(result.get(McpConstant.RESPONSE_RESULT), GetPromptResult.PromptResultDetail.class);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    @Override
    public GetPromptResult.PromptResultDetail getPrompt(String name, String arguments) {
        return getPrompt(name, JsonUtils.json2map(arguments));
    }
}
