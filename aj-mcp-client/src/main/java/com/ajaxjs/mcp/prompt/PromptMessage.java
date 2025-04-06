package com.ajaxjs.mcp.prompt;

import com.ajaxjs.mcp.McpRole;
import com.ajaxjs.mcp.message.AiMessage;
import com.ajaxjs.mcp.message.ChatMessage;
import com.ajaxjs.mcp.message.Content;
import com.ajaxjs.mcp.message.UserMessage;
import lombok.Data;

/**
 * The 'PromptMessage' object from the MCP protocol schema.
 * This can be directly translated to a ChatMessage object from the LangChain4j API.
 */

@Data
public class PromptMessage {
    McpRole role;

    PromptContent content;

    /**
     * Converts this MCP-specific PromptMessage representation to a
     * ChatMessage object from the core LangChain4j API, if possible.
     * This is currently not possible if the role is "assistant" AND
     * the content is something other than text.
     *
     * @throws UnsupportedOperationException if the role is 'assistant' and the content is something other than text.
     */
    public ChatMessage toChatMessage(McpRole role, PromptContent content) {
        if (role.equals(McpRole.USER))
            return UserMessage.userMessage(content.toContent());
        else if (role.equals(McpRole.ASSISTANT)) {
            Content convertedContent = content.toContent();

            if (convertedContent instanceof PromptTextContent) {
                PromptTextContent convertedTextContent = (PromptTextContent) convertedContent;
                return AiMessage.aiMessage(convertedTextContent.getText());
            } else {
                throw new UnsupportedOperationException("Cannot create an AiMessage with content of type " + convertedContent.getClass().getName());
            }
        } else
            throw new UnsupportedOperationException("Unknown role: " + role);
    }
}
