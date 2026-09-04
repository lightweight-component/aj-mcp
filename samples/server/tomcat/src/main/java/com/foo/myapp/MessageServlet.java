package com.foo.myapp;

import com.ajaxjs.mcp.server.ServerSse;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Represents message servlet.
 */
public class MessageServlet extends HttpServlet {
    /**
     * Holds the server sse value.
     */
    ServerSse serverSse;

    /**
     * Creates a new message servlet.
     * @param serverSse the server sse value.
     */
    public MessageServlet(ServerSse serverSse) {
        this.serverSse = serverSse;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uuid = req.getParameter("uuid");

        if (uuid == null || uuid.isEmpty())
            throw new IllegalArgumentException("The parameter 'uuid' is required.");

        String body = getBody(req);
        serverSse.handle(uuid, body);
    }

    /**
     * Executes the get body operation.
     * @param req the req value.
     * @return the result of the get body operation.
     * @throws IOException if the operation cannot complete.
     */
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
