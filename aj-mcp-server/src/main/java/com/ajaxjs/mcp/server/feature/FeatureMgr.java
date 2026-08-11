package com.ajaxjs.mcp.server.feature;

import com.ajaxjs.mcp.common.McpUtils;
import com.ajaxjs.mcp.protocol.prompt.PromptArgument;
import com.ajaxjs.mcp.protocol.prompt.PromptItem;
import com.ajaxjs.mcp.protocol.resource.ResourceItem;
import com.ajaxjs.mcp.protocol.resource.ResourceTemplate;
import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.ajaxjs.mcp.protocol.tools.JsonSchemaProperty;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.ajaxjs.mcp.server.feature.annotation.*;
import com.ajaxjs.mcp.server.feature.model.ServerStorePrompt;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResource;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;
import com.ajaxjs.mcp.server.feature.model.ServerStoreCompletion;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResourceTemplate;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FeatureMgr {
    @Getter
    private final Map<String, ServerStorePrompt> promptStore = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, ServerStoreResource> resourceStore = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, ServerStoreTool> toolStore = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, ServerStoreResourceTemplate> resourceTemplateStore = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, ServerStoreCompletion> completionStore = new ConcurrentHashMap<>();

    /**
     * Initializes all classes with a specific annotation within the given package name.
     * This method is primarily responsible for scanning all classes within the specified package and executing the corresponding processing logic based on method annotations in those classes.
     *
     * @param packageName The name of the package to scan
     */
    public void init(String packageName) {
        Set<Class<?>> classesWithAnnotation;
        log.info("Starting scanning the package of {}", packageName);

        try {
            classesWithAnnotation = PackageAnnotationScanner.findClassesWithAnnotation(packageName, McpService.class);
        } catch (IOException e) {
            log.warn("Error while scanning for classes with @McpService annotation", e);
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            log.warn("Error while scanning for classes with @McpService annotation, class not found.", e);
            throw new RuntimeException(e);
        }

        for (Class<?> clazz : classesWithAnnotation) {
            Object instance = newInstance(clazz);
            Method[] publicMethods = clazz.getDeclaredMethods();

            for (Method method : publicMethods) {
                if (method.isAnnotationPresent(Prompt.class)) {
                    addPrompt(method.getAnnotation(Prompt.class), method, instance);
                } else if (method.isAnnotationPresent(com.ajaxjs.mcp.server.feature.annotation.ResourceTemplate.class)) {
                    addResourceTemplate(method.getAnnotation(com.ajaxjs.mcp.server.feature.annotation.ResourceTemplate.class), method, instance);
                } else if (method.isAnnotationPresent(CompletePrompt.class)) {
                    addCompletion("ref/prompt", method.getAnnotation(CompletePrompt.class).value(), method, instance);
                } else if (method.isAnnotationPresent(CompleteResourceTemplate.class)) {
                    addCompletion("ref/resource", method.getAnnotation(CompleteResourceTemplate.class).value(), method, instance);
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

    private void addResourceTemplate(com.ajaxjs.mcp.server.feature.annotation.ResourceTemplate annotation,
                                     Method method, Object instance) {
        String templateName = McpUtils.isEmptyText(annotation.name()) ? method.getName() : annotation.name();
        List<String> names = new ArrayList<>();
        StringBuilder expression = new StringBuilder("^");
        String template = annotation.uriTemplate();
        int cursor = 0;
        java.util.regex.Matcher variables = java.util.regex.Pattern.compile("\\{([A-Za-z0-9_]+)}").matcher(template);
        while (variables.find()) {
            expression.append(java.util.regex.Pattern.quote(template.substring(cursor, variables.start())));
            expression.append("([^/?#]+)");
            names.add(variables.group(1));
            cursor = variables.end();
        }
        expression.append(java.util.regex.Pattern.quote(template.substring(cursor))).append('$');

        List<String> methodParameterNames = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            ResourceTemplateArg argument = parameter.getAnnotation(ResourceTemplateArg.class);
            String name = argument == null || ResourceTemplateArg.ELEMENT_NAME.equals(argument.name())
                    ? parameter.getName() : argument.name();
            methodParameterNames.add(name);
        }
        if (!new HashSet<>(names).equals(new HashSet<>(methodParameterNames)))
            throw new IllegalArgumentException("Resource template variables must match method parameters: " + templateName);

        ResourceTemplate model = new ResourceTemplate();
        model.setName(templateName);
        model.setTitle(McpUtils.isEmptyText(annotation.title()) ? null : annotation.title());
        model.setUriTemplate(template);
        model.setDescription(McpUtils.isEmptyText(annotation.description()) ? null : annotation.description());
        model.setMimeType(McpUtils.isEmptyText(annotation.mimeType()) ? null : annotation.mimeType());

        ServerStoreResourceTemplate store = new ServerStoreResourceTemplate();
        store.setInstance(instance);
        store.setMethod(method);
        store.setResourceTemplate(model);
        store.setParameterNames(methodParameterNames);
        store.setTemplateVariableNames(names);
        store.setUriPattern(java.util.regex.Pattern.compile(expression.toString()));
        resourceTemplateStore.put(templateName, store);
    }

    private void addCompletion(String referenceType, String referenceName, Method method, Object instance) {
        if (method.getParameterTypes().length != 1 || method.getParameterTypes()[0] != String.class)
            throw new IllegalArgumentException("Completion method must accept exactly one String parameter: " + method);
        CompleteArg argument = method.getParameters()[0].getAnnotation(CompleteArg.class);
        String argumentName = argument == null || McpUtils.isEmptyText(argument.name())
                ? method.getParameters()[0].getName() : argument.name();

        ServerStoreCompletion store = new ServerStoreCompletion();
        store.setInstance(instance);
        store.setMethod(method);
        store.setReferenceType(referenceType);
        store.setReferenceName(referenceName);
        store.setArgumentName(argumentName);
        completionStore.put(referenceType + ":" + referenceName + ":" + argumentName, store);
    }

    static Object newInstance(Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            log.warn("Error while instantiating class", e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            log.warn("Error while instantiating class, illegal accessing.", e);
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
        toolItem.setTitle(McpUtils.isEmptyText(tool.title()) ? null : tool.title());
        toolItem.setInputSchema(hasArgs ? inputSchema : null);
        if (!McpUtils.isEmptyText(tool.outputSchema())) {
            try {
                toolItem.setOutputSchema(com.ajaxjs.mcp.common.JsonUtils.fromJson(tool.outputSchema(), JsonSchema.class));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid outputSchema for tool " + toolName, e);
            }
        }
        Map<String, Object> annotations = new LinkedHashMap<>();
        annotations.put("readOnlyHint", tool.readOnlyHint());
        annotations.put("destructiveHint", tool.destructiveHint());
        annotations.put("idempotentHint", tool.idempotentHint());
        annotations.put("openWorldHint", tool.openWorldHint());
        toolItem.setAnnotations(annotations);

        ServerStoreTool store = new ServerStoreTool();
        store.setMethod(method);
        store.setInstance(instance);
        store.setTool(toolItem);
        store.setParamsOrder(paramsOrder);

        toolStore.put(toolName, store);
        log.info("Added tool: " + toolName);
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
                return "number";
            else if (type == Boolean.class)
                return "boolean";
            else if (type == Character.class || type == String.class)
                return "string";
        }

        // 其他类型默认返回 Object
        return "object";
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
        resourceItem.setTitle(McpUtils.isEmptyText(resource.title()) ? null : resource.title());
        resourceItem.setDescription(resource.description());
        resourceItem.setMimeType(resource.mimeType());

        ServerStoreResource store = new ServerStoreResource();
        store.setMethod(method);
        store.setInstance(instance);
        store.setResource(resourceItem);

        resourceStore.put(resource.uri(), store);
        log.info("Added resource: " + resource.uri());
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
        promptItem.setTitle(McpUtils.isEmptyText(prompt.title()) ? null : prompt.title());
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

        promptStore.put(promptName, store);
        log.info("Added prompt: {}", promptName);
    }
}
