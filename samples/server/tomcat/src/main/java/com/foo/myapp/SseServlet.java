package com.foo.myapp;

import com.ajaxjs.mcp.server.ServerSse;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@Slf4j
public class SseServlet extends HttpServlet {
    ServerSse serverSse;

    public SseServlet(ServerSse serverSse) {
        this.serverSse = serverSse;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        String uuid = UUID.randomUUID().toString();
        PrintWriter writer = null;

        try {
            writer = resp.getWriter();
            writer.write("event: endpoint\n");
            String endpointPath = "message?uuid=" + uuid;

            serverSse.addConnections(uuid, writer);
            writer.write("data: " + endpointPath + "\n\n");
            writer.flush();

            // Periodically send heartbeat messages
            while (!Thread.interrupted()) {
                try {
                    ServerSse.output(writer, "ping"); // Send a heartbeat
                    Thread.sleep(3000);// Simulate periodic updates (every 15 seconds)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            log.warn("Error on SSE handle: {}", e.getMessage());
        } finally {
            // Remove the connection when done
//            removeConnection(uuid);
//
//            if (writer != null)
//                writer.close();
        }

        // Optionally close the stream, or leave open for continuous events
    }
}