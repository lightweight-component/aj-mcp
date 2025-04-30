package com.ajaxjs.mcp.server.feature;

import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.prompt.PromptArgument;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.server.feature.annotation.*;
import com.ajaxjs.mcp.server.feature.model.ServerStorePrompt;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResource;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FeatureMgr {
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
                        addResource(resource);
                    } else {
                        Tool tool = method.getAnnotation(Tool.class);

                        if (tool != null) {
                            addTool(tool);
                        }
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

    private void addTool(Tool tool) {
    }

    private void addResource(Resource resource) {
    }

    public static final Map<String, ServerStorePrompt> PROMPT_STORE = new ConcurrentHashMap<>();

    public static final Map<String, ServerStoreResource> RESOURCE_STORE = new ConcurrentHashMap<>();

    public static final Map<String, ServerStoreTool> TOOL_STORE = new ConcurrentHashMap<>();

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
