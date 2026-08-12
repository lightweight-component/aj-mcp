---
title: MCP Server SDK 工具（Tool）开发
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK 工具（Tool）开发
date: 2022-01-05
tags:
  - 工具（Tool）开发
layout: layouts/docs-cn.njk
---
# 工具（Tool）开发

在标记了 `@McpService` 注解的类中，通过为方法添加 `@Tool` 注解来创建工具。框架会自动发现这些方法，并将其暴露为可调用的工具。

```java
@McpService
public class MyTools {
    @Tool(description = "回显字符串")
    public String echoString(@ToolArg(description = "要回显的字符串") String input) {
        return input;
    }

    @Tool("customName", description = "自定义名称工具")
    public String myMethod() {
        return "result";
    }
}
```

## 参数处理与校验

工具参数通过 @ToolArg 注解，提供用于校验和生成元数据的参数说明。每个参数可指定名称、描述及是否必填。

`@Tool` 方法的每个 Java 参数都必须标注 `@ToolArg`。缺少注解时，包扫描会立即失败，并指出类、方法和参数位置。无参数工具是合法的，其 `inputSchema` 始终为带有空 `properties` 的 object schema，不会输出 JSON `null`。

### 参数注解

| 属性         | 描述                                | 默认值        |
|--------------|-------------------------------------|---------------|
| value        | 参数名（若为空则用方法参数名）      | 空字符串      |
| description  | 面向客户端的参数描述                | 空字符串      |
| required     | 是否为必填参数                      | true          |

## 类型映射

框架自动将 Java 类型映射为 JSON Schema 类型：

| Java 类型                         | JSON Schema 类型 |
|-----------------------------------|------------------|
| String, Character                 | "string"         |
| int, long, float, double, Number  | "number"         |
| boolean, Boolean                  | "boolean"        |
| 其它类型                          | "object"         |

`required=false` 参数可以省略。缺少的引用类型参数会以 `null` 传入；Java 基本类型无法表达缺省，因此基本类型参数仍须提供。

## 工具实现示例

### 基础字符串工具

```java
@Tool(description = "回显字符串")
public String echoString(@ToolArg(description = "要回显的字符串") String input) {
    return input;
}
```

### 无参数工具

```java
@Tool(description = "耗时 5 秒完成")
public String longOperation() throws Exception {
    TimeUnit.SECONDS.sleep(5);
    return "ok";
}
```

### 返回内容工具

```java
@Tool(description = "一张图片")
public ContentImage image() {
    String base64EncodedImage = ServerUtils.encodeImageToBase64("bird-eye.jpg");
    ContentImage content = new ContentImage();
    content.setMimeType("image/jpg");
    content.setData(base64EncodedImage);
    return content;
}
```

### 多内容工具

```java
@Tool(description = "列出全部内容")
public List<Content> getAll() {
    List<Content> list = new ArrayList<>();
    list.add(new ContentImage(/* ... */));
    list.add(new ContentText("Hello World"));
    return list;
}
```

## 返回值约束

Tool 可以返回 `String`、单个 `Content`、`List<Content>`，或者在 MCP 2025-06-18 中返回 `StructuredToolResult`。SDK 会在序列化前校验 List 的每个元素。返回 `null`、null content list 或包含其它类型的 List 时，会生成 `isError=true` 的 Tool 结果，不会再触发 `NullPointerException` 或输出不合法的协议 JSON。

Prompt 方法必须返回 `PromptMessage` 或 `List<PromptMessage>`；Resource 方法必须返回 `ResourceContent` 或 `List<ResourceContent>`。非法或 null 的 Prompt/Resource 返回值会转换为 JSON-RPC `INTERNAL_ERROR`。
