package com.ajaxjs.mcp2026.core;

import java.util.Map;

/**
 * A message without transport state. Use McpCodec rather than bean serialization.
 */
public interface RpcMessage {
    /**
     * Produces the JSON-compatible JSON-RPC envelope represented by this message.
     *
     * @return envelope map for {@link McpCodec} serialization
     */
    Map<String, Object> toMap();
}
