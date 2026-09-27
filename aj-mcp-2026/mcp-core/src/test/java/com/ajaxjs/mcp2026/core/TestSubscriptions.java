package com.ajaxjs.mcp2026.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static com.ajaxjs.mcp2026.core.TestMcpCodec.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies subscription request, acknowledgement, and closure protocol helpers.
 */
class TestSubscriptions {
    private RpcRequest listen(Map<String, Object> filter) {
        return Subscriptions.listen(RequestId.of(1), RequestMetadata.create(map()), filter);
    }

    @Test
    void messagesRoundTripAndCloseCorrelates() {
        McpCodec codec = new McpCodec();
        RpcRequest request = listen(map("toolsListChanged", true));
        assertEquals(request.toMap(), codec.decode(codec.encode(request)).toMap());
        RpcNotification ack = Subscriptions.acknowledge(request, map("toolsListChanged", true));
        assertEquals(request.getId(), Subscriptions.notificationId((RpcNotification) codec.decode(codec.encode(ack))));
        assertFalse(ack.toMap().containsKey("id"));
        RpcSuccess closed = (RpcSuccess) codec.decode(codec.encode(Subscriptions.closed(request)));
        assertDoesNotThrow(() -> Subscriptions.validateClosed(request, closed));
    }

    @Test
    void flagsAndResourceUrisCannotExpandRequestedScope() {
        RpcRequest request = listen(map("toolsListChanged", false, "resourceSubscriptions", Arrays.asList("file:///a")));
        assertThrows(IllegalArgumentException.class, () -> Subscriptions.acknowledge(request, map("toolsListChanged", true)));
        assertThrows(IllegalArgumentException.class, () -> Subscriptions.acknowledge(request,
                map("resourceSubscriptions", Arrays.asList("file:///b"))));
        assertDoesNotThrow(() -> Subscriptions.acknowledge(request, map()));
    }

    @Test
    void filterSnapshotsAreImmutableAndExtensionsSurvive() {
        Map<String, Object> original = map("vendor.example/filter", map("tag", "a"));
        Map<String, Object> filter = Subscriptions.filter(original);
        original.clear();
        assertTrue(filter.containsKey("vendor.example/filter"));
        assertThrows(UnsupportedOperationException.class, () -> filter.clear());
    }

    @Test
    void malformedFiltersAreRejected() {
        for (Map<String, Object> filter : Arrays.asList(map("toolsListChanged", "true"),
                map("promptsListChanged", null), map("resourcesListChanged", 1),
                map("resourceSubscriptions", "file:///a"), map("resourceSubscriptions", Arrays.asList(1))))
            assertThrows(IllegalArgumentException.class, () -> listen(filter));
        assertThrows(IllegalArgumentException.class, () -> listen(null));
        assertThrows(IllegalArgumentException.class, () -> RpcRequest.create(RequestId.of(1),
                Subscriptions.LISTEN, map(), RequestMetadata.create(map())));
    }

    @Test
    void correlationRejectsStringNumberConfusionAndMissingMetadata() {
        RpcRequest request = listen(map());
        RpcSuccess wrong = new RpcSuccess(request.getId(), McpResult.complete(map("_meta",
                map(Subscriptions.SUBSCRIPTION_ID, "1"))));
        assertThrows(IllegalArgumentException.class, () -> Subscriptions.validateClosed(request, wrong));
        assertThrows(IllegalArgumentException.class, () -> Subscriptions.notificationId(
                new RpcNotification(Subscriptions.ACKNOWLEDGED, map("notifications", map()))));
    }
}
