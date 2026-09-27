package com.ajaxjs.mcp2026.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Bounded UTF-8 framing shared by the two transport adapters; never closes caller streams.
 */
public final class WireIO {
    /**
     * Maximum UTF-8 byte length accepted for one whole message or line.
     */
    public static final int MAX_BYTES = 4 * 1024 * 1024;

    /**
     * Prevents instantiation of stateless stream-framing helpers.
     */
    private WireIO() {
    }

    /**
     * Reads a complete input stream while enforcing the MCP message byte limit.
     *
     * @param in source stream, which remains open after this method returns
     * @return all bytes read from the stream
     * @throws IOException if reading fails or the byte limit is exceeded
     */
    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            if (out.size() > MAX_BYTES - n) throw new IOException("Message exceeds byte limit");
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * Reads one UTF-8 line without closing the input stream.
     *
     * @param in source stream containing LF- or CRLF-delimited JSON documents
     * @return decoded line without its delimiter, or {@code null} at clean EOF before a byte
     * @throws IOException if reading fails, UTF-8 is malformed, or the line is too large
     */
    public static String line(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != '\n') {
            if (out.size() >= MAX_BYTES) throw new IOException("Line exceeds byte limit");
            out.write(b);
        }
        if (b == -1 && out.size() == 0) return null;
        byte[] bytes = out.toByteArray();
        int length = bytes.length;
        if (length > 0 && bytes[length - 1] == '\r') length--;
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, 0, length)).toString();
        } catch (CharacterCodingException ex) {
            throw new IOException("Invalid UTF-8", ex);
        }
    }

    /**
     * Encodes and atomically appends one newline-delimited MCP message to a shared output stream.
     *
     * @param out     destination stream, which remains open after the write
     * @param message message to encode as one JSON-RPC document
     * @param codec   codec controlling JSON encoding and message size
     * @throws IOException if output fails
     */
    public static void writeLine(OutputStream out, RpcMessage message, McpCodec codec) throws IOException {
        byte[] bytes = codec.encodeUtf8(message);
        synchronized (out) {
            out.write(bytes);
            out.write('\n');
            out.flush();
        }
    }
}
