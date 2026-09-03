package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public  class GetResourceListResultDetail {
    @NonNull
    List<ResourceItem> resources;

    /**
     * Pagination for response.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String nextCursor;
}