package com.ajaxjs.mcp.client;

import com.ajaxjs.mcp.client.transport.McpTransport;
import com.ajaxjs.mcp.protocol.McpRequest;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequest;
import com.ajaxjs.mcp.protocol.prompt.GetPromptListRequest;
import com.ajaxjs.mcp.protocol.resource.GetResourceListRequest;
import com.ajaxjs.mcp.protocol.resource.GetResourceTemplateListRequest;
import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Represents mcp client pagination cache test.
 */
class McpClientPaginationCacheTest {
    @Test
    void promptAndResourceCachesAreScopedByPage() {
        PagingTransport transport = new PagingTransport();
        McpClient client = McpClient.builder().transport(transport).build();
        transport.start(client.pendingRequests);

        assertEquals("prompt-0", client.listPrompts().get(0).getName());
        assertEquals("prompt-2", client.listPrompts(2).get(0).getName());
        assertEquals("prompt-0", client.listPrompts().get(0).getName());

        assertEquals("resource-0", client.listResources().get(0).getName());
        assertEquals("resource-2", client.listResources(2).get(0).getName());
        assertEquals("resource-0", client.listResources().get(0).getName());

        assertEquals("template-0", client.listResourceTemplates().get(0).getName());
        assertEquals("template-2", client.listResourceTemplates(2).get(0).getName());
        assertEquals("template-0", client.listResourceTemplates().get(0).getName());

        assertEquals(1, transport.calls.get("prompts/list:0"));
        assertEquals(1, transport.calls.get("prompts/list:2"));
        assertEquals(1, transport.calls.get("resources/list:0"));
        assertEquals(1, transport.calls.get("resources/list:2"));
        assertEquals(1, transport.calls.get("resources/templates/list:0"));
        assertEquals(1, transport.calls.get("resources/templates/list:2"));
    }

    /**
     * Represents paging transport.
     */
    private static class PagingTransport extends McpTransport {
        /**
         * Holds the calls value.
         */
        private final Map<String, Integer> calls = new HashMap<>();

        @Override
        public void start(Map<Long, CompletableFuture<JsonNode>> pendingRequest) {
            setPendingRequests(pendingRequest);
        }

        @Override
        public CompletableFuture<JsonNode> initialize(InitializeRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<JsonNode> sendRequestWithResponse(McpRequest request) {
            int page = pageOf(request);
            String method = request.getMethod();
            String key = method + ":" + page;
            calls.put(key, calls.containsKey(key) ? calls.get(key) + 1 : 1);

            ObjectNode response = JsonNodeFactory.instance.objectNode();
            ObjectNode result = response.putObject("result");
            if (request instanceof GetPromptListRequest)
                result.putArray("prompts").addObject().put("name", "prompt-" + page);
            else if (request instanceof GetResourceListRequest)
                result.putArray("resources").addObject()
                        .put("uri", "file:///" + page).put("name", "resource-" + page);
            else
                result.putArray("resourceTemplates").addObject()
                        .put("uriTemplate", "file:///{id}" + page).put("name", "template-" + page);

            return CompletableFuture.completedFuture(response);
        }

        private static int pageOf(McpRequest request) {
            Cursor cursor = null;
            if (request instanceof GetPromptListRequest)
                cursor = ((GetPromptListRequest) request).getParams();
            else if (request instanceof GetResourceListRequest)
                cursor = ((GetResourceListRequest) request).getParams();
            else if (request instanceof GetResourceTemplateListRequest)
                cursor = ((GetResourceTemplateListRequest) request).getParams();

            return cursor == null ? 0 : cursor.getPageNo();
        }

        @Override
        public void sendRequestWithoutResponse(McpRequest request) {
        }

        @Override
        public void checkHealth() {
        }

        @Override
        public void close() {
        }
    }
}
