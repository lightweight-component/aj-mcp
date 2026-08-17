package com.ajaxjs.mcp.server;

import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.cancellation.CancellableTools;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSessionScopedCancellation {
    private McpServer server;

    @BeforeEach
    void setUp() {
        FeatureMgr features = new FeatureMgr();
        features.init("com.ajaxjs.mcp.server.cancellation");
        server = new McpServer();
        server.setFeatureMgr(features);
        ServerConfig config = new ServerConfig();
        config.setStrictLifecycle(false);
        server.setServerConfig(config);
    }

    @AfterEach
    void tearDown() {
        if (CancellableTools.release != null)
            CancellableTools.release.countDown();
    }

    @Test
    void cancellationIsScopedToSessionWhenRequestIdsMatch() throws Exception {
        CancellableTools.reset(2);
        CompletableFuture<String> sessionA = toolCall("session-a", 1, "blocking");
        CompletableFuture<String> sessionB = toolCall("session-b", 1, "blocking");
        assertTrue(CancellableTools.entered.await(2, TimeUnit.SECONDS));

        cancel("session-a", 1);

        assertTrue(sessionA.get(2, TimeUnit.SECONDS).contains("interrupted"));
        assertFalse(sessionB.isDone(), "cancelling session A must not interrupt session B");
        CancellableTools.release.countDown();
        assertTrue(sessionB.get(2, TimeUnit.SECONDS).contains("completed"));
    }

    @Test
    void unknownCancellationDoesNotAffectLaterRequestWithReusedId() throws Exception {
        cancel("session-a", 7);

        String response = toolCall("session-a", 7, "interruptedAtEntry").get(2, TimeUnit.SECONDS);

        assertTrue(response.contains("\"text\":\"false\""), response);
    }

    @Test
    void removingSessionInterruptsOnlyThatSessionsRequests() throws Exception {
        CancellableTools.reset(2);
        CompletableFuture<String> sessionA = toolCall("session-a", 9, "blocking");
        CompletableFuture<String> sessionB = toolCall("session-b", 9, "blocking");
        assertTrue(CancellableTools.entered.await(2, TimeUnit.SECONDS));

        server.removeSession("session-a");

        assertTrue(sessionA.get(2, TimeUnit.SECONDS).contains("interrupted"));
        assertFalse(sessionB.isDone());
        CancellableTools.release.countDown();
        assertTrue(sessionB.get(2, TimeUnit.SECONDS).contains("completed"));
    }

    private CompletableFuture<String> toolCall(String sessionId, long id, String tool) {
        return CompletableFuture.supplyAsync(() -> withSession(sessionId,
                "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"tools/call\",\"params\":{" +
                        "\"name\":\"" + tool + "\",\"arguments\":{}}}"));
    }

    private void cancel(String sessionId, long requestId) {
        withSession(sessionId, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{" +
                "\"requestId\":" + requestId + "}}");
    }

    private String withSession(String sessionId, String json) {
        server.bindSession(sessionId);
        try {
            McpResponse response = server.processMessage(McpServerInitialize.jsonRpcValidate(json));
            return response == null ? null : JsonUtils.toJson(response);
        } finally {
            server.clearSession();
        }
    }
}
