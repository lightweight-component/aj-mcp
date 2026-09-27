package com.ajaxjs.mcp2026.core;

import com.ajaxjs.mcp2026.core.json.McpJacksonEngine;
import com.ajaxjs.util.json.JsonEngine;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Strict, modern-only envelope codec. Method payload schemas and transport
 * headers are intentionally validated by separate components.
 * Injected engines must reject duplicates/trailing input and preserve numbers.
 */
public final class McpCodec {
    /**
     * Default upper bound for one complete UTF-8 JSON-RPC document (4 MiB).
     */
    public static final int DEFAULT_MAX_MESSAGE_BYTES = 4 * 1024 * 1024;

    /**
     * Instance-owned JSON engine; it is never obtained from or changed through global state.
     */
    private final JsonEngine engine;

    /**
     * Maximum permitted encoded UTF-8 document size.
     */
    private final int maxMessageBytes;

    /**
     * Core and explicitly enabled extension result types accepted by this codec.
     */
    private final Set<String> resultTypes;

    /**
     * Creates a codec with the strict default engine and 4 MiB document limit.
     */
    public McpCodec() {
        this(new McpJacksonEngine());
    }

    /**
     * Creates a codec using an instance-owned strict JSON engine.
     *
     * @param engine JSON engine that enforces strict MCP parsing requirements
     */
    public McpCodec(JsonEngine engine) {
        this(engine, DEFAULT_MAX_MESSAGE_BYTES, Collections.emptySet());
    }

    /**
     * @param engine               instance-owned engine, never changed through JsonUtil's global setter
     * @param maxMessageBytes      positive limit on the UTF-8 document
     * @param extensionResultTypes types enabled by the caller's negotiated extension policy
     */
    public McpCodec(JsonEngine engine, int maxMessageBytes, Set<String> extensionResultTypes) {
        this.engine = Objects.requireNonNull(engine, "engine");

        if (maxMessageBytes <= 0)
            throw new IllegalArgumentException("maxMessageBytes must be positive");

        this.maxMessageBytes = maxMessageBytes;
        Set<String> types = new HashSet<>(Arrays.asList(McpResult.COMPLETE, McpResult.INPUT_REQUIRED));

        for (String type : extensionResultTypes)
            types.add(Objects.requireNonNull(type, "extension result type"));

        this.resultTypes = Collections.unmodifiableSet(types);
    }

    /**
     * Serializes one MCP JSON-RPC envelope after validating an outgoing result type.
     *
     * @param message message to encode
     * @return UTF-16 JSON text whose UTF-8 representation fits the configured limit
     */
    public String encode(RpcMessage message) {
        Objects.requireNonNull(message, "message");

        if (message instanceof RpcSuccess)
            requireResultType(((RpcSuccess) message).getResult());
        if (message instanceof RpcError)
            ProtocolValidator.requireOutgoingError((RpcError) message);

        String json = engine.toJson(message.toMap());
        checkLength(json);

        return json;
    }

    /**
     * Serializes one MCP JSON-RPC envelope as UTF-8 bytes.
     *
     * @param message message to encode
     * @return encoded UTF-8 document
     */
    public byte[] encodeUtf8(RpcMessage message) {
        return encode(message).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Decodes exactly one UTF-8 MCP JSON-RPC document.
     *
     * @param bytes complete UTF-8 document within the configured byte limit
     * @return decoded request, notification, result response, or error response
     */
    public RpcMessage decodeUtf8(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");

        if (bytes.length > maxMessageBytes)
            throw new IllegalArgumentException("Message exceeds configured byte limit");

        try {
            String json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();

            return decode(json);
        } catch (CharacterCodingException e) {
            throw failure(null, false, ErrorCodes.PARSE_ERROR, "Invalid UTF-8", e);
        }
    }

    /**
     * Decodes exactly one JSON-RPC envelope without performing version or capability negotiation.
     *
     * @param json complete JSON document within the configured byte limit
     * @return decoded request, notification, result response, or error response
     */
    public RpcMessage decode(String json) {
        Objects.requireNonNull(json, "json");
        checkLength(json);
        Object parsed;

        try {
            parsed = engine.fromJson(json, Object.class);
        } catch (RuntimeException e) {
            throw failure(null, false, ErrorCodes.PARSE_ERROR, "Invalid JSON document", e);
        }

        RequestId id = null;
        boolean notification = false;
        int code = ErrorCodes.INVALID_REQUEST;

        try {
            Map<String, Object> body = WireValues.object(parsed, "JSON-RPC message");

            if (!"2.0".equals(body.get("jsonrpc")))
                throw new IllegalArgumentException("jsonrpc must be 2.0");

            if (body.containsKey("id"))
                id = RequestId.from(body.get("id"));

            if (body.containsKey("method")) {
                String method = WireValues.string(body.get("method"), "method");
                notification = !body.containsKey("id");

                if (body.containsKey("result") || body.containsKey("error"))
                    throw new IllegalArgumentException("Request cannot contain result or error");

                code = ErrorCodes.INVALID_PARAMS;

                if (notification) {
                    Map<String, Object> params = body.containsKey("params")
                            ? WireValues.object(body.get("params"), "params") : null;

                    return new RpcNotification(method, params);
                }

                return new RpcRequest(id, method, WireValues.object(body.get("params"), "params"));
            }

            if (body.containsKey("params") || body.containsKey("result") == body.containsKey("error"))
                throw new IllegalArgumentException("Response must contain exactly one of result or error");

            if (body.containsKey("result")) {
                if (id == null)
                    throw new IllegalArgumentException("Result response requires an id");

                McpResult result = McpResult.from(WireValues.object(body.get("result"), "result"));
                requireResultType(result);

                return new RpcSuccess(id, result);
            }

            Map<String, Object> error = WireValues.object(body.get("error"), "error");
            Object errorCode = RequestId.from(error.get("code")).getValue();

            if (!(errorCode instanceof BigInteger))
                throw new IllegalArgumentException("error.code must be an integer");

            int number = ((BigInteger) errorCode).intValueExact();
            String message = WireValues.string(error.get("message"), "error.message");

            return error.containsKey("data") ? new RpcError(id, number, message, error.get("data"))
                    : new RpcError(id, number, message);
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw failure(id, notification, code, code == ErrorCodes.INVALID_PARAMS
                    ? "Invalid request parameters" : "Invalid JSON-RPC message", e);
        }
    }

    /**
     * Enforces the encoded document limit for text passed into or produced by the codec.
     *
     * @param json complete JSON document
     */
    private void checkLength(String json) {
        if (json.length() > maxMessageBytes || json.getBytes(StandardCharsets.UTF_8).length > maxMessageBytes)
            throw new IllegalArgumentException("Message exceeds configured byte limit");
    }

    /**
     * Rejects result types that are neither core types nor enabled negotiated extensions.
     *
     * @param result decoded or outgoing MCP result
     */
    private void requireResultType(McpResult result) {
        if (!resultTypes.contains(result.getResultType()))
            throw new IllegalArgumentException("Unrecognized resultType");
    }

    /**
     * Creates a peer-safe protocol failure while retaining the local diagnostic as its cause.
     *
     * @param id           request identifier recovered before the failure, if any
     * @param notification whether the invalid envelope cannot receive a response
     * @param code         JSON-RPC error code
     * @param message      peer-safe error message
     * @param cause        local parsing or validation diagnostic
     * @return protocol exception carrying the sanitized wire error
     */
    private static McpProtocolException failure(RequestId id, boolean notification, int code,
                                                String message, Throwable cause) {
        return new McpProtocolException(new RpcError(id, code, message), notification, cause);
    }
}
