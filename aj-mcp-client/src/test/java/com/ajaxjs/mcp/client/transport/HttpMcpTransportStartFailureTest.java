package com.ajaxjs.mcp.client.transport;

import okhttp3.Request;
import okhttp3.sse.EventSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpMcpTransportStartFailureTest {
    @Test
    void failedEndpointInitializationCancelsCreatedEventSource() {
        FailingTransport transport = new FailingTransport();

        assertThrows(RuntimeException.class,
                () -> transport.start(new ConcurrentHashMap<>()));

        assertTrue(transport.eventSource.cancelled);
    }

    private static class FailingTransport extends HttpMcpTransport {
        private TestEventSource eventSource;

        private FailingTransport() {
            super("http://localhost/sse", false, false);
        }

        @Override
        EventSource createEventSource(Request request, SseEventListener listener) {
            eventSource = new TestEventSource(request);
            listener.onFailure(eventSource, new IOException("connection failed"), null);
            return eventSource;
        }
    }

    private static class TestEventSource implements EventSource {
        private final Request request;
        private boolean cancelled;

        private TestEventSource(Request request) {
            this.request = request;
        }

        @Override
        public Request request() {
            return request;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
