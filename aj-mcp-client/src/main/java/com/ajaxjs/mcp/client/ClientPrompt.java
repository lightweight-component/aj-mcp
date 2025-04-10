package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.prompt.GetPromptResult;
import com.ajaxjs.mcp.client.prompt.Prompt;
import com.ajaxjs.mcp.client.protocol.prompt.GetPromptRequest;
import com.ajaxjs.mcp.client.protocol.prompt.ListPromptsRequest;
import com.ajaxjs.mcp.common.IllegalResponseException;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.common.McpException;
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
public abstract class ClientPrompt extends BaseMcpClient {
    public ClientPrompt(Builder builder) {
        super(builder);
    }

    @Override
    public List<Prompt> listPrompts() {
        if (promptRefs.get() == null)
            obtainPromptList();

        return promptRefs.get();
    }

    @Override
    public GetPromptResult getPrompt(String name, Map<String, Object> arguments) {
        long operationId = idGenerator.getAndIncrement();
        GetPromptRequest operation = new GetPromptRequest(operationId, name, arguments);
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);

            return parsePromptContents(result);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operationId);
        }
    }

    private synchronized void obtainPromptList() {
        if (promptRefs.get() != null)
            return;

        ListPromptsRequest operation = new ListPromptsRequest(idGenerator.getAndIncrement());
        long timeoutMillis = requestTimeout.toMillis() == 0 ? Integer.MAX_VALUE : requestTimeout.toMillis();
        JsonNode result;
        CompletableFuture<JsonNode> resultFuture;

        try {
            resultFuture = transport.executeOperationWithResponse(operation);
            result = resultFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            promptRefs.set(parsePromptRefs(result));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            pendingOperations.remove(operation.getId());
        }
    }

    static List<Prompt> parsePromptRefs(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);

        if (mcpMessage.has("result")) {
            JsonNode resultNode = mcpMessage.get("result");

            if (resultNode.has("prompts")) {
                List<Prompt> promptRefs = new ArrayList<>();
                for (JsonNode promptNode : resultNode.get("prompts"))
                    promptRefs.add(JsonUtils.convertValue(promptNode, Prompt.class));

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

    public static GetPromptResult parsePromptContents(JsonNode mcpMessage) {
        McpException.checkForErrors(mcpMessage);
        return JsonUtils.convertValue(mcpMessage.get("result"), GetPromptResult.class);
    }
}
