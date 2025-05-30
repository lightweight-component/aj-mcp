---
title: MCP Server SDK 资源内容
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK 资源内容
date: 2022-01-05
tags:
  - 资源内容
layout: layouts/docs-cn.njk
---
# MCP 服务器 SDK 功能管理

AJ-MCP 注解体系为将 Java 方法声明式地暴露为 MCP（模型上下文协议，Model Context Protocol）工具、资源和提示提供了便捷方式。该体系通过 Java 注解自动发现和注册功能，无需手动配置或注册代码。

## 资源开发

资源通过在 `@McpService` 类中的方法上添加 `@Resource` 注解进行暴露。注解体系会在服务器初始化时自动发现和注册这些资源。

`@Resource` 注解将方法暴露为 MCP 资源，具有以下属性：

| 属性         | 是否必需 | 描述                          |
|--------------|----------|-------------------------------|
| uri          | 是       | 资源的唯一标识符              |
| description  | 否       | 人类可读的描述                |
| mimeType     | 否       | 资源内容的 MIME 类型           |
| value        | 否       | 人类可读的名称（默认为空）     |

### 资源内容类型

#### 文本资源

文本资源返回包含纯文本或结构化文本数据的 `ResourceContentText` 对象。

```java
@Resource(uri = "file:///text", description = "一段优美的文本", mimeType = "text/plain")
public ResourceContentText text(){
    ResourceContentText content = new ResourceContentText();
    content.setUri("file:///text");
    content.setMimeType("text/plain");
    content.setText("text888");
    return content;
}
```

#### 二进制资源

二进制资源返回包含 Base64 编码二进制数据的 `ResourceContentBinary` 对象。

```java
@Resource(uri = "file:///blob", description = "一张图片", mimeType = "image/jpg")
public ResourceContentBinary blob(){
    String base64EncodedImage = ServerUtils.encodeImageToBase64("bird-eye.jpg");

    ResourceContentBinary content = new ResourceContentBinary();
    content.setUri("file:///blob");
    content.setMimeType("image/jpg");
    content.setBlob(base64EncodedImage);

    return content;
}
```

### 资源模板

`@ResourceTemplate` 注解支持动态资源，采用符合 RFC 6570 URI 模板语法的参数化 URI。

| 属性         | 是否必需 | 描述                          |
|--------------|----------|-------------------------------|
| uriTemplate  | 是       | RFC 6570 Level 1 URI 模板     |
| name         | 否       | 人类可读的名称                |
| description  | 否       | 模板描述                      |
| mimeType     | 否       | 内容的 MIME 类型              |

模板方法可以接受与 URI 模板变量对应的参数。

