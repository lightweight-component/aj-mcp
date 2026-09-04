package com.ajaxjs.mcp.protocol.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Represents get tool list result tool list.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
public class GetToolListResultToolList {
    /**
     * Holds the tools value.
     */
    @NonNull
    private List<ToolItem> tools;

    /**
     * Pagination for response.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nextCursor;
}