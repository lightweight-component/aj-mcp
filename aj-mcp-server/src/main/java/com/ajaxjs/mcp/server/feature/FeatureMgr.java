package com.ajaxjs.mcp.server.feature;

import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.prompt.PromptArgument;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.ajaxjs.mcp.protocol.tools.JsonSchemaProperty;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.ajaxjs.mcp.server.feature.annotation.*;
import com.ajaxjs.mcp.server.feature.model.ServerStorePrompt;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResource;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureMgr {
    public static final Map<String, ServerStorePrompt> PROMPT_STORE = new ConcurrentHashMap<>();

    public static final Map<String, ServerStoreResource> RESOURCE_STORE = new ConcurrentHashMap<>();

    public static final Map<String, ServerStoreTool> TOOL_STORE = new ConcurrentHashMap<>();

    /**
     * Initializes all classes with a specific annotation within the given package name.
     * This method is primarily responsible for scanning all classes within the specified package and executing the corresponding processing logic based on method annotations in those classes.
     *
     * @param packageName The name of the package to scan
     */
    public void init(String packageName) {
        Set<Class<?>> classesWithAnnotation;

        try {
            classesWithAnnotation = PackageAnnotationScanner.findClassesWithAnnotation(packageName, McpService.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        for (Class<?> clazz : classesWithAnnotation) {
            Object instance = newInstance(clazz);
            Method[] publicMethods = clazz.getDeclaredMethods();

            for (Method method : publicMethods) {
                if (method.isAnnotationPresent(Prompt.class)) {
                    addPrompt(method.getAnnotation(Prompt.class), method, instance);
                } else {
                    Resource resource = method.getAnnotation(Resource.class);

                    if (resource != null) {
                        addResource(resource, method, instance);
                    } else {
                        Tool tool = method.getAnnotation(Tool.class);

                        if (tool != null)
                            addTool(tool, method, instance);
                    }
                }
            }
        }
    }

    static Object newInstance(Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a tool method to the tool store.
     * <p>
     * This method is responsible for adding methods annotated with @Tool, along with their associated metadata, to the tool store. It performs the following steps:
     * 1. Retrieves the tool name and description
     * 2. Collects and processes method parameters to generate a JSON schema for input data
     * 3. Creates and configures a ToolItem object
     * 4. Creates and configures a ServerStoreTool object
     * 5. Adds the ServerStoreTool object to the tool store
     *
     * @param tool     The instance of the class annotated with @Tool
     * @param method   The method that is marked as a tool
     * @param instance The instance object to which the method belongs
     */
    private void addTool(Tool tool, Method method, Object instance) {
        String toolName = tool.value().isEmpty() ? method.getName() : tool.value();
        String description = McpUtils.isEmptyText(tool.description()) ? null : tool.description();

        Map<String, JsonSchemaProperty> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        boolean hasArgs = false;
        Parameter[] parameters = method.getParameters();
        List<String> paramsOrder = null;

        if (parameters != null) {
            paramsOrder = new ArrayList<>();
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(ToolArg.class)) {
                    hasArgs = true;
                    ToolArg arg = parameter.getAnnotation(ToolArg.class);
                    String name = arg.value().isEmpty() ? parameter.getName() : arg.value();
                    JsonSchemaProperty property = new JsonSchemaProperty();
                    property.setType(mapJavaTypeToJsType(parameter));
                    property.setDescription(arg.description().isEmpty() ? null : arg.description());

                    properties.put(name, property);
                    paramsOrder.add(name);

                    if (arg.required())
                        required.add(name);
                }
            }
        }
        JsonSchema inputSchema = new JsonSchema();

        if (hasArgs) {
            inputSchema.setType("object");
            inputSchema.setProperties(properties);
            inputSchema.setRequired(required);
        }

        ToolItem toolItem = new ToolItem();
        toolItem.setName(toolName);
        toolItem.setDescription(description);
        toolItem.setInputSchema(hasArgs ? inputSchema : null);

        ServerStoreTool store = new ServerStoreTool();
        store.setMethod(method);
        store.setInstance(instance);
        store.setTool(toolItem);
        store.setParamsOrder(paramsOrder);

        TOOL_STORE.put(toolName, store);
    }

    /**
     * Maps Java parameter types to JavaScript types.
     * This method is primarily used to convert between Java types and JavaScript types, ensuring correct data handling on the front end.
     *
     * @param parameter The parameter object of a Java method, used to obtain type information about the parameter
     * @return A string representing the corresponding JavaScript type
     */
    public static String mapJavaTypeToJsType(Parameter parameter) {
        // 获取参数的类型
        Class<?> type = parameter.getType();

        // 基础类型和包装类型的映射
        if (type.isPrimitive()) {
            if (type == byte.class || type == short.class || type == int.class ||
                    type == long.class || type == float.class || type == double.class)
                return "number";
            else if (type == boolean.class)
                return "boolean";
            else if (type == char.class)
                return "string";
        } else {
            if (Number.class.isAssignableFrom(type))
                return "Number";
            else if (type == Boolean.class)
                return "Boolean";
            else if (type == Character.class || type == String.class)
                return "string";
        }

        // 其他类型默认返回 Object
        return "Object";
    }

    /**
     * Adds resource information to the resource store.
     * <p>
     * This method is responsible for encapsulating the given resource information, method, and instance object into resource items and server storage resource objects,
     * then storing them in the resource store. This method demonstrates how to associate metadata with business logic components,
     * making it easier to access and use this information in subsequent processing.
     *
     * @param resource The resource annotation containing metadata about the resource
     * @param method   The method object of the class to which the resource belongs
     * @param instance The instance object of the class to which the resource belongs
     */
    private void addResource(Resource resource, Method method, Object instance) {
        ResourceItem resourceItem = new ResourceItem();
        resourceItem.setUri(resource.uri());
        resourceItem.setName(resource.value().isEmpty() ? method.getName() : resource.value());
        resourceItem.setDescription(resource.description());
        resourceItem.setMimeType(resource.mimeType());

        ServerStoreResource store = new ServerStoreResource();
        store.setMethod(method);
        store.setInstance(instance);
        store.setResource(resourceItem);

        RESOURCE_STORE.put(resource.uri(), store);
    }

    /**
     * Adds a prompt to the prompt store.
     * <p>
     * This method is responsible for building a prompt item from a method and instance, and storing it in the prompt store.
     * It processes the method's annotations to extract prompt-related information, including the prompt's name, description, and arguments.
     *
     * @param prompt   The annotation instance containing the prompt definition.
     * @param method   The method where the prompt is defined.
     * @param instance The instance of the class containing the method.
     */
    private void addPrompt(Prompt prompt, Method method, Object instance) {
        String promptName = prompt.value().isEmpty() ? method.getName() : prompt.value();
        String description = prompt.description();

        List<PromptArgument> arguments = null;
        Parameter[] parameters = method.getParameters();

        if (parameters != null)
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(PromptArg.class)) {
                    PromptArg arg = parameter.getAnnotation(PromptArg.class);
                    PromptArgument argument = new PromptArgument();
                    argument.setName(arg.value().isEmpty() ? parameter.getName() : arg.value());

                    if (McpUtils.isEmptyText(argument.getName()))
                        throw new IllegalArgumentException("The name of prompt is required!");

                    argument.setDescription(arg.description());
                    argument.setRequired(arg.required());

                    if (arguments == null)
                        arguments = new ArrayList<>();

                    arguments.add(argument);
                }
            }

        PromptItem promptItem = new PromptItem();
        promptItem.setName(promptName);
        promptItem.setDescription(description);
        promptItem.setArguments(arguments);

//        Class<?> returnType = method.getReturnType();
//        List<PromptMessage> messages = null;
//        if (returnType == List.class) {
//
//        } else {
//            messages = Arrays.asList();
//        }
//        GetPromptResultDetail.PromptResultDetail result = new GetPromptResultDetail.PromptResultDetail();
//        result.setDescription(description);
//        result.setMessages(messages);

        ServerStorePrompt store = new ServerStorePrompt();
        store.setMethod(method);
        store.setInstance(instance);
        store.setPrompt(promptItem);

        PROMPT_STORE.put(promptName, store);
    }
}
