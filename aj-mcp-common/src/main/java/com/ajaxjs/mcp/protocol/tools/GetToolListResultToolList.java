package com.ajaxjs.mcp.protocol.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
public class GetToolListResultToolList {
    @NonNull
    private List<ToolItem> tools;

    /**
     * Pagination for response.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nextCursor;
}