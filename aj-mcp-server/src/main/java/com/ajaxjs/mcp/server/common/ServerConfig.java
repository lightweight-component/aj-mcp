package com.ajaxjs.mcp.server.common;

import com.ajaxjs.mcp.protocol.ProtocolVersion;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.time.Duration;

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

    /** Finite default for server-to-client calls when no positive timeout is supplied. */
    private Duration clientRequestTimeout = Duration.ofSeconds(60);

    /** Idle HTTP sessions expire even when no GET stream was opened. */
    private Duration sessionIdleTimeout = Duration.ofMinutes(30);

    /** Maximum simultaneously executing STDIO requests. */
    private int stdioWorkers = 16;

    /** Maximum queued STDIO requests before returning a server-busy error. */
    private int stdioQueueCapacity = 256;

    /**
     * Allowed browser Origin values for Streamable HTTP. Empty rejects every supplied Origin.
     */
    private List<String> allowedOrigins = Collections.emptyList();
}
