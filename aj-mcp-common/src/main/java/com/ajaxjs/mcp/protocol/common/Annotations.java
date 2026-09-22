package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.prompt.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

/** Content/resource hints, distinct from tool behavior annotations and never an authorization policy. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Annotations {
    private List<Role> audience;
    private Double priority;
    /** RFC 3339 timestamp; retained verbatim to avoid losing timezone or precision. */
    private String lastModified;
}
