package com.foo.streamable;

import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.model.HttpResult;
import org.springframework.web.bind.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** HTTP adaptation only; JSON-RPC dispatch and session policy remain in the SDK. */
@RestController
public class McpController {
    private final ServerStreamableHttp transport;
    private final RequestContext context;

    public McpController(ServerStreamableHttp transport, RequestContext context) {
        this.transport = transport; this.context = context;
    }

    @PostMapping("/mcp")
    public void post(@RequestBody(required = false) String body, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String json = body == null ? "" : body;
        context.bind(request.getHeader(ServerStreamableHttp.SESSION_ID_HEADER), json);

        try {
            write(transport.post(json, headers(request)), response);
        } finally { context.clear(); }
    }

    @DeleteMapping("/mcp")
    public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(transport.delete(request.getHeader(ServerStreamableHttp.SESSION_ID_HEADER), headers(request)), response);
    }

    @GetMapping("/mcp")
    public void get(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        AsyncContext async = request.startAsync();
        async.setTimeout(0); // Session expiry and heartbeats are owned by ServerStreamableHttp.
        String session = request.getHeader(ServerStreamableHttp.SESSION_ID_HEADER);
        AtomicBoolean ended = new AtomicBoolean();
        PrintWriter writer = new PrintWriter(response.getWriter()) {
            @Override public void close() {
                if (ended.compareAndSet(false, true)) {
                    super.close();
                    try { async.complete(); } catch (IllegalStateException ignored) { }
                }
            }
        };

        async.addListener(new AsyncListener() {
            private void cleanup() {
                if (session != null) transport.closeEventStream(session, writer);
                writer.close();
            }

            @Override public void onComplete(AsyncEvent event) { cleanup(); }
            @Override public void onTimeout(AsyncEvent event) { cleanup(); }
            @Override public void onError(AsyncEvent event) { cleanup(); }
            @Override public void onStartAsync(AsyncEvent event) { }
        });
        try {
            // Prevent a concurrent heartbeat from committing headers before they are copied.
            synchronized (transport) {
                HttpResult result = transport.openEventStream(session, writer, headers(request));
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("X-Accel-Buffering", "no");
                write(result, response);
                response.flushBuffer();

                if (result.getStatus() != 200)
                    writer.close();
            }
        } catch (IOException | RuntimeException failure) {
            if (session != null)
                transport.closeEventStream(session, writer);

            writer.close();

            throw failure;
        }
    }

    private static Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> values.put(name, request.getHeader(name)));

        return values;
    }

    private static void write(HttpResult result, HttpServletResponse response) throws IOException {
        response.setStatus(result.getStatus());
        response.setCharacterEncoding("UTF-8");
        result.getHeaders().forEach(response::setHeader);

        if (result.getContentType() != null)
            response.setContentType(result.getContentType());

        // A notification has no JSON body, not the literal string "null".
        if (result.getBody() != null)
            response.getWriter().write(result.getBody());
    }
}
