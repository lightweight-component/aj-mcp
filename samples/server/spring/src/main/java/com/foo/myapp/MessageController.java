package com.foo.myapp;

import com.ajaxjs.mcp.server.ServerSse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Represents message controller.
 */
@RestController
public class MessageController {
    /**
     * Holds the server sse value.
     */
    @Autowired
    ServerSse serverSse;

    /**
     * Executes the handle json operation.
     * @param json the json value.
     * @param uuid the uuid value.
     */
    @PostMapping("/message")
    public void handleJson(@RequestBody String json, @RequestParam String uuid) {
        System.out.println("Received JSON: " + json);
        serverSse.handle(uuid, json);
    }
}
