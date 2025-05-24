package com.foo.myapp;

import com.ajaxjs.mcp.server.ServerSse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@RestController
public class SseController {
    @Autowired
    ServerSse serverSse;

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamSse(HttpServletRequest req, HttpServletResponse resp) {
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
            System.err.println("Error on SSE handle:" + e.getMessage());
            e.printStackTrace();
        } finally {
            // Remove the connection when done
//            removeConnection(uuid);
//
//            if (writer != null)
//                writer.close();
        }
    }
}