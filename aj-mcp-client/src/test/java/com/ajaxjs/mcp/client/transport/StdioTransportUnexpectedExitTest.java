package com.ajaxjs.mcp.client.transport;

import com.ajaxjs.mcp.protocol.utils.ping.PingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StdioTransportUnexpectedExitTest {
    @Test
    @Timeout(5)
    void unexpectedProcessExitFailsPendingRequestImmediately() throws Exception {
        StdioTransport transport = StdioTransport.builder()
                .command(Arrays.asList("/bin/sh", "-c", "sleep 0.2; exit 0"))
                .build();
        Map<Long, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();

        try {
            transport.start(pending);
            PingRequest request = new PingRequest();
            request.setId(1L);
            CompletableFuture<JsonNode> response = transport.sendRequestWithResponse(request);

            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> response.get(2, TimeUnit.SECONDS));

            assertInstanceOf(IOException.class, error.getCause());
            assertTrue(pending.isEmpty());
        } finally {
            transport.close();
        }
    }
}
