package com.foo.myapp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class MessageServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        String body = getBody(req);
        System.out.println(body);

        PrintWriter writer = resp.getWriter();
        writer.write("id: " + 1 + "\n");
        writer.write("event: message\n"); // This is the "type"
        String endpointPath = "aaaaaaaaaaaaaa";
//            String json = JsonUtils.toJson(McpUtils.mapOf("kk", "endpointd"));
        writer.write("data: " + endpointPath + "\n\n");
        writer.flush();
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