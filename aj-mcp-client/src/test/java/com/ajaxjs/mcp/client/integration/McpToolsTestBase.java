package com.ajaxjs.mcp.client.integration;

import com.ajaxjs.mcp.client.McpClient;
import com.ajaxjs.mcp.client.McpToolProvider;
import com.ajaxjs.mcp.client.json.JsonBooleanSchema;
import com.ajaxjs.mcp.client.json.JsonIntegerSchema;
import com.ajaxjs.mcp.client.json.JsonStringSchema;
import com.ajaxjs.mcp.tool.ToolExecutionRequest;
import com.ajaxjs.mcp.tool.ToolExecutor;
import com.ajaxjs.mcp.tool.ToolProviderResult;
import com.ajaxjs.mcp.tool.ToolSpecification;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public abstract class McpToolsTestBase {
    static McpClient mcpClient;

    @Test
    public void verifyToolSpecifications() {
        ToolProviderResult toolProviderResult = obtainTools();

        assertEquals(6, toolProviderResult.size(), "Expected exactly 6 tools");

        // Verify echoString tool
        ToolSpecification echoString = findToolSpecificationByName(toolProviderResult, "echoString");
        assertNotNull(echoString, "Tool 'echoString' should not be null");
        assertEquals("Echoes a string", echoString.getDescription(), "Description mismatch for 'echoString'");
        JsonStringSchema echoStringParam = (JsonStringSchema) echoString.getParameters().getProperties().get("input");
        assertNotNull(echoStringParam, "Parameter 'input' for 'echoString' should not be null");
        assertEquals("The string to be echoed", echoStringParam.getDescription(), "Description mismatch for 'echoString' parameter");

        // Verify echoInteger tool
        ToolSpecification echoInteger = findToolSpecificationByName(toolProviderResult, "echoInteger");
        assertNotNull(echoInteger, "Tool 'echoInteger' should not be null");
        assertEquals("Echoes an integer", echoInteger.getDescription(), "Description mismatch for 'echoInteger'");
        JsonIntegerSchema echoIntegerParam = (JsonIntegerSchema) echoInteger.getParameters().getProperties().get("input");
        assertNotNull(echoIntegerParam, "Parameter 'input' for 'echoInteger' should not be null");
        assertEquals("The integer to be echoed", echoIntegerParam.getDescription(), "Description mismatch for 'echoInteger' parameter");

        // Verify echoBoolean tool
        ToolSpecification echoBoolean = findToolSpecificationByName(toolProviderResult, "echoBoolean");
        assertNotNull(echoBoolean, "Tool 'echoBoolean' should not be null");
        assertEquals("Echoes a boolean", echoBoolean.getDescription(), "Description mismatch for 'echoBoolean'");
        JsonBooleanSchema echoBooleanParam = (JsonBooleanSchema) echoBoolean.getParameters().getProperties().get("input");
        assertNotNull(echoBooleanParam, "Parameter 'input' for 'echoBoolean' should not be null");
        assertEquals("The boolean to be echoed", echoBooleanParam.getDescription(), "Description mismatch for 'echoBoolean' parameter");

        // Verify longOperation tool
        ToolSpecification longOperation = findToolSpecificationByName(toolProviderResult, "longOperation");
        assertNotNull(longOperation, "Tool 'longOperation' should not be null");
        assertEquals("Takes 10 seconds to complete", longOperation.getDescription(), "Description mismatch for 'longOperation'");
        assertTrue(longOperation.getParameters().getProperties().isEmpty(), "Parameters for 'longOperation' should be empty");

        // Verify error tool
        ToolSpecification error = findToolSpecificationByName(toolProviderResult, "error");
        assertNotNull(error, "Tool 'error' should not be null");
        assertEquals("Throws a business error", error.getDescription(), "Description mismatch for 'error'");
        assertTrue(error.getParameters().getProperties().isEmpty(), "Parameters for 'error' should be empty");

        // Verify errorResponse tool
        ToolSpecification errorResponse = findToolSpecificationByName(toolProviderResult, "errorResponse");
        assertNotNull(errorResponse, "Tool 'errorResponse' should not be null");
        assertEquals("Returns a response as an error", errorResponse.getDescription(), "Description mismatch for 'errorResponse'");
        assertTrue(errorResponse.getParameters().getProperties().isEmpty(), "Parameters for 'errorResponse' should be empty");
    }

    @Test
    public void executeTool() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = findToolExecutorByName(toolProviderResult, "echoString");
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name("echoString")
                .arguments("{\"input\": \"abc\"}")
                .build();
        String toolExecutionResultString = executor.execute(toolExecutionRequest, null);
        assertEquals("abc", toolExecutionResultString);
    }

    @Test
    public void executeToolWithWrongArgumentType() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = findToolExecutorByName(toolProviderResult, "echoString");
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name("echoString")
                .arguments("{\"input\": 1}") // wrong argument type
                .build();
        String toolExecutionResultString = executor.execute(toolExecutionRequest, null);
        assertEquals("There was an error executing the tool. Message: Internal error. Code: -32603", toolExecutionResultString);
    }

    @Test
    public void executeNonExistentTool() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = findToolExecutorByName(toolProviderResult, "echoString");
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name("THIS-TOOL-DOES-NOT-EXIST")
                .arguments("{\"input\": 1}")
                .build();
        String toolExecutionResultString = executor.execute(toolExecutionRequest, null);
        assertEquals("There was an error executing the tool. " + "Message: Invalid tool name: THIS-TOOL-DOES-NOT-EXIST. Code: -32602", toolExecutionResultString);
    }

    @Test
    public void executeToolThatThrowsBusinessError() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = findToolExecutorByName(toolProviderResult, "error");
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder().name("error").arguments("{}").build();
        String toolExecutionResultString = executor.execute(toolExecutionRequest, null);
        assertEquals("There was an error executing the tool. Message: Internal error. Code: -32603", toolExecutionResultString);
    }

    @Test
    public void executeToolThatReturnsError() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = findToolExecutorByName(toolProviderResult, "errorResponse");
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name("errorResponse")
                .arguments("{}")
                .build();
        String toolExecutionResultString = executor.execute(toolExecutionRequest, null);
        assertEquals("There was an error executing the tool. The tool returned: This is an actual error", toolExecutionResultString);
    }

    @Test
    public void timeout() {
        ToolProviderResult toolProviderResult = obtainTools();
        ToolExecutor executor = findToolExecutorByName(toolProviderResult, "longOperation");
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name("longOperation")
                .arguments("{}")
                .build();
        String toolExecutionResultString = executor.execute(toolExecutionRequest, null);
        assertEquals("There was a timeout executing the tool", toolExecutionResultString);
    }

    ToolProviderResult obtainTools() {
        McpToolProvider toolProvider = new McpToolProvider();
        toolProvider.setMcpClients(Collections.singletonList(mcpClient));

        return toolProvider.provideTools(null);
    }

    ToolSpecification findToolSpecificationByName(ToolProviderResult toolProviderResult, String name) {
        return toolProviderResult.keySet().stream()
                .filter(toolSpecification -> toolSpecification.getName().equals(name))
                .findFirst()
                .get();
    }

    ToolExecutor findToolExecutorByName(ToolProviderResult toolProviderResult, String name) {
        return toolProviderResult.entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(name))
                .findFirst()
                .get()
                .getValue();
    }
}
