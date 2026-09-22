package com.ajaxjs.mcp.protocol.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A filesystem root exposed by an MCP client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Root extends com.ajaxjs.mcp.protocol.common.Metadata {
    /**
     * Holds the uri value.
     */
    private String uri;

    /**
     * Holds the name value.
     */
    private String name;
}
