package com.ajaxjs.mcp.message;

/**
 * A factory for creating {@link ChatMessageJsonCodec} objects.
 * Used for SPI.
 */
public interface ChatMessageJsonCodecFactory {
    /**
     * Creates a new {@link ChatMessageJsonCodec} object.
     *
     * @return the new {@link ChatMessageJsonCodec} object.
     */
    ChatMessageJsonCodec create();
}
