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
    /**
     * Protocol metadata advertised by {@code resources/templates/list}.
     */
    private ResourceTemplate resourceTemplate;
    /**
     * Java method parameter names in invocation order.
     */
    private List<String> parameterNames;
    /**
     * URI-template variable names extracted from the RFC 6570 template.
     */
    private List<String> templateVariableNames;
    /**
     * Compiled regular expression used to match incoming resource URIs and extract variables.
     */
    private Pattern uriPattern;
}
