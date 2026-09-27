package com.ajaxjs.mcp2026.core;

import com.ajaxjs.mcp2026.core.schema.SchemaValidator;

import java.util.Collections;
import java.util.Map;

/**
 * Compile once per registered tool; no business invocation or shared registry.
 */
public final class ToolContract {
    /**
     * Descriptor whose name and schemas define this contract.
     */
    private final ToolDefinition tool;
    /**
     * Compiled input schema evaluated before invoking application code.
     */
    private final SchemaValidator.CompiledSchema input;
    /**
     * Optional compiled output schema evaluated for non-error structured results.
     */
    private final SchemaValidator.CompiledSchema output;
    /**
     * Shared validator for enclosing MCP request and result envelopes.
     */
    private final ProtocolSchema protocol;

    /**
     * Compiles a tool's input and optional output schemas for reusable invocation validation.
     *
     * @param tool      complete registered tool descriptor
     * @param validator schema compiler used for the tool-local schemas
     * @param protocol  validator used for MCP method envelopes
     * @throws IllegalArgumentException if the descriptor or a schema is invalid
     */
    public ToolContract(ToolDefinition tool, SchemaValidator validator, ProtocolSchema protocol) {
        this.tool = java.util.Objects.requireNonNull(tool, "tool");
        this.protocol = java.util.Objects.requireNonNull(protocol, "protocol");
        protocol.requireType("Tool", tool.toMap());
        input = validator.compile(WireValues.object(tool.toMap().get("inputSchema"), "inputSchema"));
        output = tool.toMap().containsKey("outputSchema")
                ? validator.compile(WireValues.object(tool.toMap().get("outputSchema"), "outputSchema")) : null;
    }

    /**
     * Maps invalid arguments to a protocol error before any business code is called.
     *
     * @param request candidate {@code tools/call} request
     * @throws McpProtocolException if the request targets another tool or violates inputSchema
     */
    public void requireCall(RpcRequest request) {
        protocol.requireRequest(request);
        if (!"tools/call".equals(request.getMethod()) || !tool.getName().equals(request.getParams().get("name")))
            throw invalid(request, "Unknown tool");
        Object arguments = request.getParams().containsKey("arguments")
                ? request.getParams().get("arguments") : Collections.emptyMap();
        if (!input.validate(arguments).isEmpty())
            throw invalid(request, "Tool arguments do not satisfy inputSchema");
    }

    /**
     * Successful structured output must satisfy outputSchema, including legal JSON null.
     *
     * @param request  originating {@code tools/call} request
     * @param response correlated handler success response
     * @throws McpProtocolException     if the request itself is invalid
     * @throws IllegalArgumentException if the response violates the MCP or output-schema contract
     */
    public void requireResult(RpcRequest request, RpcSuccess response) {
        requireCall(request);
        protocol.requireResult(request, response);
        if (!McpResult.COMPLETE.equals(response.getResult().getResultType()))
            return;
        Map<String, Object> fields = response.getResult().toMap();
        if (output != null && !Boolean.TRUE.equals(fields.get("isError"))) {
            if (!fields.containsKey("structuredContent") || !output.validate(fields.get("structuredContent")).isEmpty())
                throw new IllegalArgumentException("Tool result does not satisfy outputSchema");
        }
    }

    /**
     * Creates a correlated peer-safe invalid-parameters failure.
     *
     * @param request offending request
     * @param message safe validation message
     * @return protocol exception carrying the request identifier
     */
    private McpProtocolException invalid(RpcRequest request, String message) {
        return new McpProtocolException(new RpcError(request.getId(), ErrorCodes.INVALID_PARAMS, message), false, null);
    }
}
