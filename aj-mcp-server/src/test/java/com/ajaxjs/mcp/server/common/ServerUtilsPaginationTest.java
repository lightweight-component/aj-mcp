package com.ajaxjs.mcp.server.common;

import com.ajaxjs.mcp.protocol.utils.pagination.Cursor;
import com.ajaxjs.mcp.server.McpServer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression coverage for shared list-detail pagination.
 */
class ServerUtilsPaginationTest {
    @Test
    void paginatedDetailUsesTheConfiguredPageAndSetsTheNextCursor() {
        McpServer server = serverWithPageSize(2);
        AtomicReference<String> nextCursor = new AtomicReference<>();

        List<Integer> detail = ServerUtils.paginatedDetail(Arrays.asList(1, 2, 3), new Cursor(1), server,
                items -> items, (ignored, next) -> nextCursor.set(next));

        assertEquals(Arrays.asList(1, 2), detail);
        assertEquals("{\"page\":2}", new String(java.util.Base64.getDecoder().decode(nextCursor.get())));
    }

    @Test
    void paginatedDetailKeepsTheWholeListWhenNoCursorWasRequested() {
        McpServer server = serverWithPageSize(2);
        AtomicReference<String> nextCursor = new AtomicReference<>();

        List<Integer> detail = ServerUtils.paginatedDetail(Arrays.asList(1, 2, 3), null, server,
                items -> items, (ignored, next) -> nextCursor.set(next));

        assertEquals(Arrays.asList(1, 2, 3), detail);
        assertNull(nextCursor.get());
    }

    private static McpServer serverWithPageSize(int pageSize) {
        McpServer server = new McpServer();
        ServerConfig config = new ServerConfig();
        config.setPageSize(pageSize);
        server.setServerConfig(config);
        return server;
    }
}
