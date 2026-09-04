package com.ajaxjs.mcp.protocol.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Represents get prompt list result detail.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
public class GetPromptListResultDetail {
    /**
     * Holds the prompts value.
     */
    @NonNull
    private List<PromptItem> prompts;

    /**
     * Pagination for response.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nextCursor;
}