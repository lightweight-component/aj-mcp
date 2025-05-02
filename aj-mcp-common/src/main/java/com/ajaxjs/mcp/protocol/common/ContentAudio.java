package com.ajaxjs.mcp.protocol.common;

import com.ajaxjs.mcp.protocol.McpConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The audio data MUST be base64-encoded and include a valid MIME type. This enables multi-modal interactions where audio context is important.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContentAudio extends Content {
    /**
     * base64-encoded-audio-data
     */
    String data;

    /**
     * MimeType, like "audio/wav"
     */
    String mimeType;

    public ContentAudio() {
        this.type = McpConstant.ContentType.AUDIO;
    }
}
