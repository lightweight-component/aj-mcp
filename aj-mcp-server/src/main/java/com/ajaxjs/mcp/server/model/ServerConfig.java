package com.ajaxjs.mcp.server.model;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ServerConfig {
    /**
     * The name of the server.
     */
    private String name;

    /**
     * Supported protocol versions.
     */
    private List<String> protocolVersions = Collections.singletonList("2024-11-05");
}
