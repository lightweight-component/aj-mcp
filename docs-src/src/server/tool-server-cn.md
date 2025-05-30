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
| 其它类型                          | "Object"         |

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
@Tool(description = "耗时 10 秒完成")
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