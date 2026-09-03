package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceTemplatesResultDetail {
    List<ResourceTemplate> resourceTemplates;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String nextCursor;
}