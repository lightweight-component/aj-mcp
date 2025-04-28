package com.ajaxjs.mcp.protocol.prompt;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The audio data MUST be base64-encoded and include a valid MIME type. This enables multi-modal interactions where audio context is important.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptContentAudio extends PromptContent {
    /**
     * base64-encoded-audio-data
     */
    String data;

    /**
     * MimeType, like "audio/wav"
     */
    String mimeType;
}
