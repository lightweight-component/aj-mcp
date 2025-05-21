package com.foo.myapp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class SseServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        System.out.println(req.getRequestURI());

        if ("/sse".equals(req.getRequestURI())) {
            PrintWriter writer = resp.getWriter();
            writer.write("id: " + 1 + "\n");
            writer.write("event: endpoint\n"); // This is the "type"
            String endpointPath = "message";
//            String json = JsonUtils.toJson(McpUtils.mapOf("kk", "endpointd"));
            writer.write("data: " + endpointPath + "\n\n");
            writer.flush();
        } else {

            // Example: sending 5 events, 1 per second
            for (int i = 1; i <= 5; i++) {
                resp.getWriter().write("data: Event " + i + "\n\n");
                resp.getWriter().flush();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            // Optionally close the stream, or leave open for continuous events
        }
    }
}