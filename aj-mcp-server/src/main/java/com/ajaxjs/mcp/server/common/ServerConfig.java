package com.ajaxjs.mcp.server.common;

import com.ajaxjs.mcp.protocol.ProtocolVersion;
import lombok.Data;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Runtime configuration for an {@code McpServer} instance.
 * <p>
 * The values in this object describe the server implementation advertised during
 * initialization, pagination defaults shared by list operations, protocol versions accepted
 * during negotiation, and transport-level safety limits such as lifecycle enforcement,
 * idle-session expiry, and STDIO back pressure.
 */
@Data
public class ServerConfig {
    /**
     * Stable implementation name advertised in the initialize response.
     */
    private String name;

    /**
     * Optional human-readable implementation label. It is emitted only for peers that
     * negotiated the 2025-06-18 protocol revision or later behavior supported by this SDK.
     */
    private String title;

    /**
     * Implementation version string advertised in the initialize response.
     */
    private String version;

    /**
     * Default number of tools, prompts, resources, or templates returned by cursor-based list APIs.
     */
    private Integer pageSize = 3;

    /**
     * Protocol revisions this server is willing to negotiate, ordered by preference.
     */
    private List<String> protocolVersions = ProtocolVersion.supportedVersions();

    /**
     * Whether each transport session must complete {@code initialize} and {@code initialized}
     * before ordinary feature requests are accepted.
     */
    private boolean strictLifecycle = true;

    /**
     * Finite default timeout for reverse server-to-client requests when the caller does not
     * provide a positive timeout.
     */
    private Duration clientRequestTimeout = Duration.ofSeconds(60);

    /**
     * Maximum time an HTTP session may remain idle before the Streamable HTTP transport
     * releases it, even when no optional GET stream was opened.
     */
    private Duration sessionIdleTimeout = Duration.ofMinutes(30);

    /**
     * Maximum number of STDIO JSON-RPC requests that may execute concurrently.
     */
    private int stdioWorkers = 16;

    /**
     * Maximum number of accepted but not yet executing STDIO requests before the server
     * responds with a server-busy error.
     */
    private int stdioQueueCapacity = 256;

    /**
     * Allowed browser Origin values for Streamable HTTP and legacy SSE HTTP adapters.
     * Empty rejects every supplied Origin; native clients may omit the header.
     */
    private List<String> allowedOrigins = Collections.emptyList();
}
