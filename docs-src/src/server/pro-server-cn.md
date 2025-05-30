---
title: MCP Server SDK Prompts Development
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK Prompts Development
date: 2022-01-05
tags:
  - Prompts Development
layout: layouts/docs-cn.njk
---
# 提示（Prompt）开发

AJ-MCP 通过注解在 `@McpService` 类中定义提示（Prompt）。`@Prompt` 注解用于标记作为提示处理器的方法，`@PromptArg` 用于定义交互式提示的参数。

简单提示返回单条 `PromptMessage`，内容为文本。`@Prompt` 注解要求提供描述，方法返回配置好的消息（含角色和内容）。

```java
@Prompt(description = "基础简单提示")
public PromptMessage basic(){
    PromptMessage message = new PromptMessage();
    message.setRole(Role.USER);
    message.setContent(new ContentText("Hello"));

    return message;
}
```

## 多消息提示

提示可以返回多条消息（`List<PromptMessage>`），支持多轮对话或多角色交互场景。

```java
@Prompt(description = "返回两条消息的提示")
public List<PromptMessage> multi(){
    PromptMessage message1 = new PromptMessage();
    message1.setRole(Role.USER);
    message1.setContent(new ContentText("first"));

    PromptMessage message2 = new PromptMessage();
    message2.setRole(Role.USER);
    message2.setContent(new ContentText("second"));

    return Arrays.asList(message1, message2);
}
```

## 带参数提示

提示可通过 `@PromptArg` 注解接收参数。参数会在列出提示时对客户端可见，并在获取提示时传递。

```java
@Prompt(description = "带参数的提示")
public PromptMessage parametrized(@PromptArg(description = "姓名") String name){
    PromptMessage message = new PromptMessage();
    message.setRole(Role.USER);
    message.setContent(new ContentText("Hello " + name));

    return message;
}
```

## 内容类型与富媒体支持

提示不仅支持文本，还支持图片和嵌入式资源等多种内容类型。每种内容类型有特定属性和编码要求。

| 内容类型           | 类名                      | 关键属性                          | 用途               |
|--------------------|---------------------------|-----------------------------------|--------------------|
| 文本               | ContentText               | text                              | 普通文本消息       |
| 图片               | ContentImage              | data, mimeType                    | Base64 编码图片    |
| 嵌入式资源         | ContentEmbeddedResource   | resource.uri, resource.mimeType   | 二进制数据引用     |

## 协议数据结构

提示系统采用特定协议结构进行客户端与服务器之间的数据通信。

| 类名              | 作用                            | 关键字段                                 |
|-------------------|---------------------------------|------------------------------------------|
| PromptItem        | 提示元数据（用于列表）          | name, description, arguments             |
| PromptMessage     | 单条消息内容                    | role, content                            |
| GetPromptResult   | 响应包装                        | result.description, result.messages      |
| PromptArgument    | 参数定义                        | name, description, required              |

## 角色枚举

Role 枚举用于定义提示消息中的角色：

- USER —— 用户视角的消息
- ASSISTANT —— AI 助手视角的消息
- SYSTEM —— 系统指令或上下文消息
