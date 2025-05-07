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

    private void addTool(Tool tool, Method method, Object instance) {
        String toolName = tool.value().isEmpty() ? method.getName() : tool.value();
        String description = McpUtils.isEmptyText(tool.description()) ? null : tool.description();

        Map<String, JsonSchemaProperty> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        boolean hasArgs = false;
        Parameter[] parameters = method.getParameters();
        List<String> paramsOrder=null;

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
            else if (type == Character.class)
                return "string";
            else if (type == String.class)
                return "string";
        }

        // 其他类型默认返回 Object
        return "Object";
    }

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
