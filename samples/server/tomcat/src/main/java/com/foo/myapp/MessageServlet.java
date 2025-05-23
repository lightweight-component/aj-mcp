package com.foo.myapp;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.transport.McpTransportSync;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class MessageServlet extends HttpServlet {
    McpServer server;

    SseServlet sseServlet;

    public MessageServlet(McpServer server, SseServlet sseServlet) {
        this.server = server;
        this.sseServlet = sseServlet;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uuid = req.getParameter("uuid");

        if (uuid == null || uuid.isEmpty())
            throw new IllegalArgumentException("The parameter 'uuid' is required.");

        String body = getBody(req);
        System.out.println(body);

        McpTransportSync transport = server.getTransport();
        String data = transport.handle(body);
        sseServlet.returnMessage(uuid, data);
    }

    static String getBody(HttpServletRequest req) throws IOException {
        StringBuilder requestBody = new StringBuilder();

        try (BufferedReader reader = req.getReader()) {
            String line;

            while ((line = reader.readLine()) != null)
                requestBody.append(line).append("\n");
        }

        return requestBody.toString();
    }
}