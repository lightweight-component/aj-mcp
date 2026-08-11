package com.ajaxjs.mcp.server.feature.model;

import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Runtime binding between an RFC 6570 level-1 template and its Java method.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerStoreResourceTemplate extends ServerStoreBase {
    private ResourceTemplate resourceTemplate;
    private List<String> parameterNames;
    private List<String> templateVariableNames;
    private Pattern uriPattern;
}
