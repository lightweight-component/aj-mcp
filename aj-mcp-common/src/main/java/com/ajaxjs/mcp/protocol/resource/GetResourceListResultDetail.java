package com.ajaxjs.mcp.protocol.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Represents get resource list result detail.
 */
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class GetResourceListResultDetail {
    /**
     * Holds the resources value.
     */
    @NonNull
    List<ResourceItem> resources;

    /**
     * Pagination for response.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String nextCursor;
}