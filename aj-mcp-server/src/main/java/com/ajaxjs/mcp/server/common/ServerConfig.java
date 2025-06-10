package com.ajaxjs.mcp.server.common;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Config object
 */
@Data
public class ServerConfig {
    /**
     * The name of the server.
     */
    private String name;

    /**
     * The version of the server.
     */
    private String version;

    /**
     * The page size.
     */
    private Integer pageSize = 3;

    /**
     * Supported protocol versions.
     */
    private List<String> protocolVersions = Collections.singletonList("2024-11-05");
}
