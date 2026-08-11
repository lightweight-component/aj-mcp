package com.ajaxjs.mcp.server.common;

import lombok.Data;

import java.util.Collections;
import java.util.List;

import com.ajaxjs.mcp.protocol.ProtocolVersion;

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
    private List<String> protocolVersions = ProtocolVersion.supportedVersions();

    /**
     * Enforces the MCP initialize/initialized lifecycle for every transport session.
     */
    private boolean strictLifecycle = true;

    /**
     * Allowed browser Origin values for Streamable HTTP. Empty rejects every supplied Origin.
     */
    private List<String> allowedOrigins = Collections.emptyList();
}
