package com.foo.myapp;

import com.ajaxjs.mcp.server.ServerSse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {
    @Autowired
    ServerSse serverSse;

    @PostMapping("/message")
    public void handleJson(@RequestBody String json, @RequestParam String uuid) {
        System.out.println("Received JSON: " + json);
        String data = serverSse.handle(json);
        serverSse.returnMessage(uuid, data);
    }
}
