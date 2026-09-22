package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.tools.ToolItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Runtime binding for one exposed MCP tool.
 * <p>
 * It couples the protocol-facing {@link ToolItem} metadata with the Java method that performs
 * the work and records parameter order for reflective invocation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreTool extends ServerStoreBase {
    /**
     * Tool metadata returned by {@code tools/list} and used to validate calls.
     */
    ToolItem tool;

    /**
     * The order of the parameters.
     * When passing the arguments, it's a map. so we need to transform the map to array, then pass it to the Java method.
     */
    List<String> paramsOrder;
}
