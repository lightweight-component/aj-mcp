package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
public class GetPromptListResultDetail {
    @NonNull
    private List<PromptItem> prompts;

    /**
     * Pagination for response.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nextCursor;
}