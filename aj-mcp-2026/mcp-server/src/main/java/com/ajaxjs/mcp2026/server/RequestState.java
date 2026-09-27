package com.ajaxjs.mcp2026.server;

import com.ajaxjs.mcp2026.core.*;
import com.ajaxjs.mcp2026.core.json.McpJacksonEngine;
import com.ajaxjs.util.cryptography.DoCipher;
import com.ajaxjs.util.cryptography.aes.AesCipherResult;
import com.ajaxjs.util.cryptography.aes.AesGcm;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.util.*;

/**
 * Authenticated, confidential MRTR state using AJ Util AES-GCM. Not an authorization or replay ledger.
 */
public final class RequestState {
    /**
     * Maximum accepted encoded token length, limiting decoder and decryption work.
     */
    private static final int MAX_TOKEN_CHARS = 65536;

    /**
     * AES-GCM primitive holding a private clone of the configured 128-bit key.
     */
    private final AesGcm aes;

    /**
     * Stable deployment binding that prevents state tokens crossing server audiences.
     */
    private final String audience;

    /**
     * Clock used for both issuing and enforcing token expiry.
     */
    private final Clock clock;

    /**
     * Strict JSON engine used to serialize authenticated state snapshots.
     */
    private final McpJacksonEngine json = new McpJacksonEngine();

    /**
     * Creates state protection with the system UTC clock.
     *
     * @param key      random 128-bit AES key
     * @param audience stable server-side audience binding
     */
    public RequestState(byte[] key, String audience) {
        this(key, audience, Clock.systemUTC());
    }

    /**
     * Creates state protection with an injectable clock for bounded expiry validation.
     *
     * @param key      random 128-bit AES key
     * @param audience stable server-side audience binding
     * @param clock    clock used to issue and validate expiry timestamps
     */
    public RequestState(byte[] key, String audience, Clock clock) {
        if (key == null || key.length != 16)
            throw new IllegalArgumentException("Use a random 128-bit AES key");

        this.aes = new AesGcm(key.clone());
        this.audience = Objects.requireNonNull(audience);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Generates a fresh random AES-128 key suitable for one server state-protection configuration.
     *
     * @return 16 random key bytes
     */
    public static byte[] newKey() {
        return DoCipher.randomBytes(16);
    }

    /**
     * Encrypts opaque MRTR state and binds it to the request's stable parameters, server audience, and principal.
     *
     * @param original  original request whose retry may carry the resulting state
     * @param principal authenticated principal, if any
     * @param state     JSON state to preserve between MRTR rounds
     * @param ttlMs     positive lifetime no greater than one day
     * @return versioned encrypted request-state token
     */
    public String seal(RpcRequest original, Principal principal, Map<String, Object> state, long ttlMs) {
        if (ttlMs <= 0 || ttlMs > 86400000)
            throw new IllegalArgumentException("State TTL must be within one day");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expires", Math.addExact(clock.millis(), ttlMs));
        payload.put("state", state);
        String text = json.toJson(McpResult.complete(payload).toMap());

        if (text.getBytes(StandardCharsets.UTF_8).length > 45000)
            throw new IllegalArgumentException("State too large");

        AesCipherResult encrypted = aes.encrypt(text, binding(original, principal));

        return "v1." + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted.getNonce().getResult()) + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted.getResult());
    }

    /**
     * Authenticates, decrypts, and returns state from an MRTR retry.
     * Invalid, expired, incorrectly bound, or oversize tokens become a peer-safe {@code -32602} protocol error.
     *
     * @param retry     retry request carrying {@code requestState}
     * @param principal authenticated principal, if any
     * @return restored JSON state
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> open(RpcRequest retry, Principal principal) {
        try {
            Object raw = retry.getParams().get("requestState");

            if (!(raw instanceof String) || ((String) raw).length() > MAX_TOKEN_CHARS)
                throw new IllegalArgumentException("Missing state");

            String[] parts = ((String) raw).split("\\.", -1);

            if (parts.length != 3 || !"v1".equals(parts[0]))
                throw new IllegalArgumentException("Invalid state version");

            byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
            String encrypted = Base64.getEncoder().encodeToString(Base64.getUrlDecoder().decode(parts[2]));
            Map<String, Object> payload = json.fromJson(aes.decrypt(encrypted, nonce, binding(retry, principal)), Map.class);

            if (!(payload.get("expires") instanceof Number) || ((Number) payload.get("expires")).longValue() <= clock.millis())
                throw new IllegalArgumentException("Expired state");

            if (!(payload.get("state") instanceof Map))
                throw new IllegalArgumentException("Invalid state payload");

            return (Map<String, Object>) McpResult.complete(Collections.singletonMap("state", payload.get("state"))).toMap().get("state");
        } catch (RuntimeException ex) {
            throw new McpProtocolException(new RpcError(retry.getId(), ErrorCodes.INVALID_PARAMS, "Invalid or expired request state"), false, ex);
        }
    }

    /**
     * Produces canonical authenticated data binding a token to the stable request, audience, and principal.
     * Retry-only fields are excluded because they necessarily change between MRTR rounds.
     *
     * @param request   original or retry request
     * @param principal authenticated principal, if any
     * @return UTF-8 canonical binding bytes for AES-GCM authenticated encryption
     */
    private byte[] binding(RpcRequest request, Principal principal) {
        Map<String, Object> params = new TreeMap<>(request.getParams());
        params.remove("requestState");
        params.remove("inputResponses");
        Map<String, Object> data = new TreeMap<>();
        data.put("format", "aj-mcp-next-state-v1");
        data.put("audience", audience);
        data.put("principal", principal == null ? null : principal.getName());
        data.put("method", request.getMethod());
        data.put("params", params);

        return json.toJson(canonical(data)).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Canonical JSON-compatible values so equivalent request bindings serialize deterministically.
     *
     * @param value JSON value to normalize
     * @return recursively sorted and normalized JSON-compatible value
     */
    private Object canonical(Object value) {
        if (value instanceof Map) {
            Map<String, Object> result = new TreeMap<>();
            ((Map<?, ?>) value).forEach((k, v) -> result.put((String) k, canonical(v)));

            return result;
        }

        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object v : (List<?>) value) result.add(canonical(v));

            return result;
        }

        if (value instanceof Number)
            return new BigDecimal(value.toString()).stripTrailingZeros();

        return value;
    }
}
