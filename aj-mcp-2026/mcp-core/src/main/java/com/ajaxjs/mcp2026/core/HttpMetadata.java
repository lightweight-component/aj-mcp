package com.ajaxjs.mcp2026.core;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Pure HTTP header/body binding rules; owns no connections or transport state.
 */
public final class HttpMetadata {
    /**
     * Prevents instantiation of stateless HTTP metadata helpers.
     */
    private HttpMetadata() {
    }

    /**
     * Derives all protocol-required HTTP headers from a request body and optional tool descriptor.
     *
     * @param request validated request whose metadata and parameters are mirrored
     * @param tool    descriptor for a {@code tools/call}, or {@code null} for other methods
     * @return case-insensitive map of required header names and encoded values
     * @throws IllegalArgumentException if a required mirrored request value is absent or invalid
     */
    public static Map<String, String> headers(RpcRequest request, ToolDefinition tool) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("MCP-Protocol-Version", request.getMetadata().getProtocolVersion());
        headers.put("Mcp-Method", request.getMethod());
        String key = "resources/read".equals(request.getMethod()) ? "uri"
                : ("tools/call".equals(request.getMethod()) || "prompts/get".equals(request.getMethod())) ? "name" : null;
        if (key != null) headers.put("Mcp-Name", encode((String) request.getParams().get(key)));
        if (tool != null && "tools/call".equals(request.getMethod())) {
            for (Binding binding : bindings(tool)) {
                Object value = request.getParams().get("arguments");
                for (String segment : binding.path)
                    value = value instanceof Map ? ((Map<?, ?>) value).get(segment) : null;
                if (value != null) headers.put("Mcp-Param-" + binding.name, encode(binding.text(value)));
            }
        }
        return headers;
    }

    /**
     * Validates the {@code x-mcp-header} declarations that a tool exposes in its input schema.
     *
     * @param tool tool descriptor to inspect
     * @throws IllegalArgumentException if a declaration is ambiguous, unreachable, or unsupported
     */
    public static void validateTool(ToolDefinition tool) {
        bindings(tool);
    }

    /**
     * Verifies that incoming HTTP headers exactly bind to the request body.
     *
     * @param request  decoded request body
     * @param tool     resolved tool descriptor for a tool call, or {@code null}
     * @param incoming raw inbound headers grouped by name
     * @throws McpProtocolException if required headers are absent, duplicated, unsafe, or mismatched
     */
    public static void require(RpcRequest request, ToolDefinition tool, Map<String, List<String>> incoming) {
        try {
            Map<String, List<String>> actual = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            incoming.forEach((k, v) -> {
                if (k != null && actual.put(k, v) != null) throw new IllegalArgumentException("Duplicate header");
            });
            Map<String, String> expected = headers(request, tool);
            Set<String> integerHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            if (tool != null) for (Binding binding : bindings(tool)) {
                String key = "Mcp-Param-" + binding.name;
                if (!expected.containsKey(key) && actual.containsKey(key))
                    throw new IllegalArgumentException("Unexpected header");
                if ("integer".equals(binding.type)) integerHeaders.add(key);
            }
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                List<String> values = actual.get(entry.getKey());
                if (values == null || values.size() != 1)
                    throw new IllegalArgumentException("Missing or duplicate header");
                String supplied = values.get(0);
                if (!safe(supplied)) throw new IllegalArgumentException("Invalid header");
                String left = entry.getKey().equalsIgnoreCase("Mcp-Name") || entry.getKey().startsWith("Mcp-Param-")
                        ? decode(supplied) : supplied;
                String right = entry.getKey().equalsIgnoreCase("Mcp-Name") || entry.getKey().startsWith("Mcp-Param-")
                        ? decode(entry.getValue()) : entry.getValue();
                boolean equal = integerHeaders.contains(entry.getKey())
                        ? new BigDecimal(left).compareTo(new BigDecimal(right)) == 0 : left.equals(right);
                if (!equal) throw new IllegalArgumentException("Header mismatch");
            }
        } catch (RuntimeException ex) {
            throw new McpProtocolException(new RpcError(request.getId(), ErrorCodes.HEADER_MISMATCH,
                    "Required HTTP headers are missing or do not match the body"), false, ex);
        }
    }

    /**
     * Converts a header value to an ASCII-safe representation without losing UTF-8 data.
     *
     * @param value logical header value
     * @return unmodified safe value or the MCP base64 sentinel representation
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String encode(String value) {
        Objects.requireNonNull(value, "header value");
        return safe(value) && !sentinel(value) ? value : "=?base64?" +
                Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    /**
     * Decodes the MCP base64 sentinel form when present.
     *
     * @param value raw ASCII header value
     * @return decoded logical value, or {@code value} when no sentinel is present
     * @throws IllegalArgumentException if a sentinel value has invalid Base64 or UTF-8 content
     */
    public static String decode(String value) {
        if (!sentinel(value)) return value;
        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(9, value.length() - 2));
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid encoded header", ex);
        }
    }

    /**
     * Determines whether a value uses the MCP base64 header sentinel.
     *
     * @param s header text to inspect
     * @return {@code true} when the text has the sentinel prefix and suffix
     */
    private static boolean sentinel(String s) {
        return s.startsWith("=?base64?") && s.endsWith("?=");
    }

    /**
     * Checks whether a value can be sent directly as one safe HTTP field value.
     *
     * @param s candidate header text
     * @return {@code true} for printable ASCII without leading/trailing whitespace
     */
    private static boolean safe(String s) {
        if (s == null || !s.equals(s.trim())) return false;
        for (int i = 0; i < s.length(); i++)
            if ((s.charAt(i) < 32 && s.charAt(i) != 9) || s.charAt(i) > 126) return false;
        return true;
    }

    /**
     * Extracts reachable input-schema fields marked for HTTP header mirroring.
     *
     * @param tool tool descriptor containing the input schema
     * @return ordered validated header bindings
     * @throws IllegalArgumentException if a schema declaration is not safe to mirror
     */
    private static List<Binding> bindings(ToolDefinition tool) {
        List<Binding> result = new ArrayList<>();
        scan(tool.toMap().get("inputSchema"), new ArrayList<>(), true, result, new HashSet<>());
        return result;
    }

    /**
     * Recursively scans schema nodes while retaining only unambiguous object-property paths.
     *
     * @param value     current schema value
     * @param path      property path from the root input object
     * @param reachable whether the path represents a required direct object traversal
     * @param result    destination collection for valid bindings
     * @param names     case-insensitive names already claimed by a binding
     * @throws IllegalArgumentException if a header declaration violates the transport contract
     */
    private static void scan(Object value, List<String> path, boolean reachable, List<Binding> result, Set<String> names) {
        if (value instanceof List) {
            for (Object item : (List<?>) value) scan(item, path, false, result, names);
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.containsKey("x-mcp-header")) {
                Object raw = map.get("x-mcp-header");
                String name = raw instanceof String ? (String) raw : "";
                Object type = map.get("type");
                if (!reachable || path.isEmpty() || !name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
                        || !Arrays.asList("string", "integer", "boolean").contains(type)
                        || !names.add(name.toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Invalid x-mcp-header");
                result.add(new Binding(name, (String) type, path));
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if ("properties".equals(entry.getKey()) && reachable && entry.getValue() instanceof Map && !map.containsKey("$ref")) {
                    for (Map.Entry<?, ?> property : ((Map<?, ?>) entry.getValue()).entrySet()) {
                        List<String> child = new ArrayList<>(path);
                        child.add((String) property.getKey());
                        scan(property.getValue(), child, true, result, names);
                    }
                } else if (!"x-mcp-header".equals(entry.getKey())) scan(entry.getValue(), path, false, result, names);
            }
        }
    }

    /**
     * Validated mapping from an input-schema property path to an MCP parameter header.
     */
    private static final class Binding {
        /**
         * Header suffix and JSON Schema type of the mirrored value.
         */
        final String name, type;
        /**
         * Immutable property path used to retrieve the argument value.
         */
        final List<String> path;

        /**
         * Creates an immutable binding after schema validation has established the path and type.
         *
         * @param name HTTP header suffix declared by {@code x-mcp-header}
         * @param type supported JSON Schema scalar type
         * @param path direct object-property path to the argument
         */
        Binding(String name, String type, List<String> path) {
            this.name = name;
            this.type = type;
            this.path = new ArrayList<>(path);
        }

        /**
         * Converts one JSON scalar to its canonical mirrored-header text.
         *
         * @param value argument value at this binding's property path
         * @return canonical string representation suitable for header encoding
         * @throws IllegalArgumentException if the runtime value does not match the declared type
         */
        String text(Object value) {
            if ("string".equals(type) && value instanceof String || "boolean".equals(type) && value instanceof Boolean)
                return value.toString();
            if ("integer".equals(type) && value instanceof Number) {
                BigDecimal n = new BigDecimal(value.toString());
                if (n.stripTrailingZeros().scale() <= 0 && n.abs().compareTo(new BigDecimal("9007199254740991")) <= 0)
                    return n.toBigIntegerExact().toString();
            }
            throw new IllegalArgumentException("Invalid mirrored parameter value");
        }
    }
}
