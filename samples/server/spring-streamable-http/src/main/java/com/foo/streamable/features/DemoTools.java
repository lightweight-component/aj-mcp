package com.foo.streamable.features;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.annotation.*;
import com.foo.streamable.RequestContext;
import java.time.Duration;
import java.time.Instant;

@McpService
public class DemoTools {
    private McpServer server;
    private RequestContext context;
    public void configure(McpServer server, RequestContext context) { this.server = server; this.context = context; }

    @Tool(description = "Returns the current UTC time", readOnlyHint = true)
    public String serverTime() { return Instant.now().toString(); }

    @Tool(title = "Greeting", description = "Greets a name, or the world if omitted", readOnlyHint = true)
    public String greet(@ToolArg(value = "name", required = false) String name) {
        return "Hello, " + (name == null ? "world" : name) + "!";
    }

    @Tool(description = "Reports three progress updates without external work", readOnlyHint = true)
    public String progressDemo() {
        Object token = context.progressToken();

        if (token != null)
            for (int step = 1; step <= 3; step++)
                server.sendProgress(token, step, 3.0, "Completed step " + step);

        return "Completed 3 steps";
    }

    @Tool(description = "Requests the client's advertised roots; does not read files", readOnlyHint = true)
    public String clientRoots() {
        return server.listRoots(context.sessionId(), Duration.ofSeconds(5)).toString();
    }
}
