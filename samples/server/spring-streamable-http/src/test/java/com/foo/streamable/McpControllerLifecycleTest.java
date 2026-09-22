package com.foo.streamable;

import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.model.HttpResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.Collections;
import static org.mockito.Mockito.*;

class McpControllerLifecycleTest {
    @Test void sdkWriterCloseCompletesAsyncAndCallbacksDetachByIdentity() throws Exception {
        ServerStreamableHttp transport = mock(ServerStreamableHttp.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AsyncContext async = mock(AsyncContext.class);
        when(request.startAsync()).thenReturn(async);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("Mcp-Session-Id")));
        when(request.getHeader("Mcp-Session-Id")).thenReturn("session");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        when(transport.openEventStream(eq("session"), any(), any()))
                .thenReturn(new HttpResult(200, Collections.emptyMap(), "text/event-stream", null));
        new McpController(transport, new RequestContext()).get(request, response);
        ArgumentCaptor<PrintWriter> writer = ArgumentCaptor.forClass(PrintWriter.class);
        verify(transport).openEventStream(eq("session"), writer.capture(), any());
        ArgumentCaptor<AsyncListener> listener = ArgumentCaptor.forClass(AsyncListener.class);
        verify(async).addListener(listener.capture());
        writer.getValue().close(); writer.getValue().close();
        verify(async, times(1)).complete();
        listener.getValue().onComplete(new AsyncEvent(async));
        listener.getValue().onError(new AsyncEvent(async));
        listener.getValue().onTimeout(new AsyncEvent(async));
        verify(transport, times(3)).closeEventStream("session", writer.getValue());
        verify(async, times(1)).complete();
    }
}
