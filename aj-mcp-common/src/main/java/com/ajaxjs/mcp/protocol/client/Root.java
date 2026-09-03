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
public class Root {
    private String uri;

    private String name;
}
