package com.foo.myapp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SseServlet extends HttpServlet {
    Map<String, PrintWriter> connections = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        System.out.println(req.getRequestURI());

        if ("/sse".equals(req.getRequestURI())) {
            PrintWriter writer = resp.getWriter();
            writer.write("event: endpoint\n");

            String uuid = UUID.randomUUID().toString();
            String endpointPath = "message?uuid=" + uuid;

            connections.put(uuid, writer);
            writer.write("data: " + endpointPath + "\n\n");
            writer.flush();

            // Periodically send heartbeat messages
            while (!Thread.interrupted()) {
                try {
                    // Send a heartbeat
                    writer.write("data: ping\n\n");
                    writer.flush();

                    // Simulate periodic updates (every 15 seconds)
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {

            // Optionally close the stream, or leave open for continuous events
        }
    }

    public void returnMessage(String uuid, String data) {
        PrintWriter writer = connections.get(uuid);

        if (writer == null)
            throw new IllegalStateException("Connection id: " + uuid + " is not found.");

//        writer.write("id: " + 2 + "\n");
        writer.write("event: message\n");
        String endpointPath = "message";
        writer.write("data: " + data + "\n\n");
        writer.flush();
    }
}