package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.tools.ToolItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreTool extends ServerStoreBase {
    ToolItem tool;

    /**
     * The order of the parameters.
     * When passing the arguments, it's a map. so we need to transform the map to array, then pass it to the Java method.
     */
    List<String> paramsOrder;
}
