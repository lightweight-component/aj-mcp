package com.ajaxjs.mcp2026.core.schema;

import com.ajaxjs.mcp2026.core.json.McpJacksonEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * JSON-only, offline JSON Schema adapter. Compiled plans own their schema snapshots.
 * Limits bound input structure, not wall-clock time of arbitrary regular expressions.
 */
public final class DefaultSchemaValidator implements SchemaValidator {
    /**
     * URI used by JSON Schema draft-07 documents.
     */
    public static final String DRAFT_7 = "http://json-schema.org/draft-07/schema#";
    /**
     * Meta-schema resources allowed during offline validation; all other remote fetches are disabled.
     */
    private static final Set<String> META_RESOURCES = new HashSet<>(Arrays.asList(
            DEFAULT_DIALECT, "https://json-schema.org/draft/2020-12/meta/core",
            "https://json-schema.org/draft/2020-12/meta/applicator",
            "https://json-schema.org/draft/2020-12/meta/unevaluated",
            "https://json-schema.org/draft/2020-12/meta/validation",
            "https://json-schema.org/draft/2020-12/meta/meta-data",
            "https://json-schema.org/draft/2020-12/meta/format-annotation",
            "https://json-schema.org/draft/2020-12/meta/content",
            "http://json-schema.org/draft-07/schema"));
    /**
     * Strict JSON mapper shared only by this validator instance.
     */
    private final McpJacksonEngine engine = new McpJacksonEngine();
    /**
     * Offline registry configured with only known JSON Schema meta-resources.
     */
    private final SchemaRegistry registry;
    /**
     * Maximum permitted map/list nesting depth for schemas and instances.
     */
    private final int maxDepth;
    /**
     * Maximum visited schema or instance nodes during one snapshot.
     */
    private final int maxNodes;

    /**
     * Creates an offline validator with a depth of 64 and a 20,000-node structure limit.
     */
    public DefaultSchemaValidator() {
        this(64, 20000);
    }

    /**
     * Limits apply to both schema and instance JSON trees, including scalar nodes.
     *
     * @param maxDepth maximum nested object/array depth from one root
     * @param maxNodes maximum visited values, keys, and container nodes
     * @throws IllegalArgumentException if the limits cannot provide a bounded validator
     */
    public DefaultSchemaValidator(int maxDepth, int maxNodes) {
        if (maxDepth < 1 || maxDepth > 64 || maxNodes < 1)
            throw new IllegalArgumentException("Invalid validation limits");
        this.maxDepth = maxDepth;
        this.maxNodes = maxNodes;
        registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12, builder -> builder
                .nodeReader(reader -> reader.jsonMapper(engine.OBJECT_MAPPER))
                .schemaLoader(loader -> loader.fetchRemoteResources(false)
                        .allow(iri -> META_RESOURCES.contains(iri.toString()))));
    }

    @Override
    public CompiledSchema compile(Map<String, Object> schema) {
        if (schema == null)
            throw new IllegalArgumentException("Schema must be an object");
        JsonNode node = snapshot(schema);
        String dialect = node.has("$schema") ? node.get("$schema").asText() : DEFAULT_DIALECT;
        if (!DEFAULT_DIALECT.equals(dialect) && !DRAFT_7.equals(dialect))
            throw new IllegalArgumentException("Unsupported JSON Schema dialect");
        try {
            Schema meta = registry.getSchema(SchemaLocation.of(dialect));
            if (!meta.validate(node).isEmpty())
                throw new IllegalArgumentException("Invalid JSON Schema");
            checkDirectRecursion(node, node, Collections.newSetFromMap(new java.util.IdentityHashMap<>()),
                    Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
            Schema compiled = registry.getSchema(node);
            // Eagerly surface unresolved references instead of accepting dormant invalid branches.
            compiled.initializeValidators();
            return instance -> violations(compiled, snapshot(instance));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot compile JSON Schema (invalid dialect, schema or reference)", e);
        }
    }

    /**
     * Converts library diagnostics into safe MCP-facing violation summaries.
     *
     * @param schema   compiled library schema
     * @param instance immutable JSON tree to validate
     * @return immutable list of location-and-keyword diagnostics
     */
    private List<SchemaViolation> violations(Schema schema, JsonNode instance) {
        List<SchemaViolation> violations = new ArrayList<>();
        for (com.networknt.schema.Error error : schema.validate(instance)) {
            // Library messages may contain rejected values. Expose location and keyword only.
            violations.add(new SchemaViolation(error.getInstanceLocation().toString(), error.getKeyword(),
                    "JSON Schema constraint failed"));
        }
        return Collections.unmodifiableList(violations);
    }

    /**
     * Rejects non-progressing local pointer cycles through same-instance applicators.
     *
     * @param node    current schema node
     * @param root    root schema node used to resolve local pointers
     * @param active  nodes on the current recursive path
     * @param checked nodes completely inspected in an earlier path
     * @throws IllegalArgumentException if a non-progressing cycle or depth overflow is found
     */
    private void checkDirectRecursion(JsonNode node, JsonNode root, Set<JsonNode> active, Set<JsonNode> checked) {
        if (!node.isObject() || checked.contains(node))
            return;
        if (active.size() >= maxDepth)
            throw new IllegalArgumentException("Schema reference depth limit exceeded");
        if (!active.add(node))
            throw new IllegalArgumentException("Non-progressing local schema reference cycle");
        try {
            // A nested resource has its own reference base; let the engine resolve it.
            if (node != root && node.has("$id"))
                return;
            JsonNode ref = node.get("$ref");
            if (ref != null && ref.isTextual()) {
                String pointer = ref.textValue();
                if ("#".equals(pointer))
                    checkDirectRecursion(root, root, active, checked);
                else if (pointer.startsWith("#/"))
                    checkDirectRecursion(root.at(java.net.URI.create(pointer).getFragment()), root, active, checked);
            }
            for (String keyword : Arrays.asList("allOf", "anyOf", "oneOf")) {
                JsonNode children = node.get(keyword);
                if (children != null)
                    for (JsonNode child : children)
                        checkDirectRecursion(child, root, active, checked);
            }
            for (String keyword : Arrays.asList("not", "if", "then", "else")) {
                JsonNode child = node.get(keyword);
                if (child != null)
                    checkDirectRecursion(child, root, active, checked);
            }
            checked.add(node);
        } finally {
            active.remove(node);
        }
    }

    /**
     * Validates JSON shape and produces a detached Jackson tree for schema-library processing.
     *
     * @param value schema or instance value to snapshot
     * @return detached JSON tree preserving Java number precision
     * @throws IllegalArgumentException if the value is not bounded JSON-compatible data
     */
    private JsonNode snapshot(Object value) {
        inspect(value, 0, new int[]{0, 0});
        return engine.OBJECT_MAPPER.valueToTree(value);
    }

    /**
     * Walks a JSON-compatible value and enforces structural, numeric, and character limits.
     *
     * @param value value currently being inspected
     * @param depth current recursive depth
     * @param count two-element node and character counters
     * @throws IllegalArgumentException if the value is invalid or exceeds a configured limit
     */
    private void inspect(Object value, int depth, int[] count) {
        if (depth > maxDepth || ++count[0] > maxNodes)
            throw new IllegalArgumentException("JSON validation structure limit exceeded");
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String))
                    throw new IllegalArgumentException("JSON object keys must be strings");
                addCharacters(((String) entry.getKey()).length(), count);
                inspect(entry.getValue(), depth + 1, count);
            }
        } else if (value instanceof List) {
            for (Object child : (List<?>) value)
                inspect(child, depth + 1, count);
        } else if (value instanceof String) {
            addCharacters(((String) value).length(), count);
        } else if (value instanceof Double || value instanceof Float) {
            if (!Double.isFinite(((Number) value).doubleValue()))
                throw new IllegalArgumentException("Non-finite JSON number");
        } else if (value != null && !(value instanceof Boolean) && !(value instanceof Byte)
                && !(value instanceof Short) && !(value instanceof Integer) && !(value instanceof Long)
                && !(value instanceof BigInteger) && !(value instanceof BigDecimal)) {
            throw new IllegalArgumentException("Expected a JSON value");
        }
        if (value instanceof Number) {
            int length = value.toString().length();
            if (length > 1000)
                throw new IllegalArgumentException("JSON number limit exceeded");
            addCharacters(length, count);
        }
    }

    /**
     * Adds a string or number representation length to the shared character budget.
     *
     * @param length non-negative character count to add
     * @param count  two-element node and character counters
     * @throws IllegalArgumentException if the aggregate character budget would be exceeded
     */
    private void addCharacters(int length, int[] count) {
        if (length > 4 * 1024 * 1024 - count[1])
            throw new IllegalArgumentException("JSON validation character limit exceeded");
        count[1] += length;
    }
}
