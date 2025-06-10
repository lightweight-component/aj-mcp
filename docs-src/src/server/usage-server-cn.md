---
title: MCP Server SDK 用法指南
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK 用法指南
date: 2022-01-05
tags:
  - MCP Server SDK 用法指南
layout: layouts/docs-cn.njk
---
# MCP 服务器 SDK 使用说明

## MCP 服务器 SDK 安装

添加如下依赖以构建 MCP 服务器：

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.2</version>
</dependency>
```

可在此处查看最新版本：
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-server?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)

服务端模块包含：

- `McpServer` 核心处理引擎
- 基于注解的功能发现管理器 `FeatureMgr`
- `@Tool`、`@Resource`、`@Prompt` 等注解
- HTTP/SSE 与标准输入输出（Stdio）传输实现

## 创建服务器

要创建 MCP 服务器，需要以下步骤：

1. 定义服务类：创建带有 `@McpService` 注解的类
2. 注解方法：使用 `@Tool`、`@Prompt` 或 `@Resource` 等注解标记方法
3. 初始化功能管理器：扫描包以发现注解
4. 配置传输层：设置 HTTP/SSE 或 Stdio 传输，并配置相关服务器参数
5. 启动服务器：调用 `server.start()`

## 创建 MCP 服务类

AJ-MCP 通过注解扫描自动发现、注册和管理 MCP 功能（工具、资源、提示）。开发者只需在带有 `@McpService` 注解的类中，使用 `@Tool`、`@Resource` 或 `@Prompt` 注解标记方法，即可暴露相应功能。

```java
@McpService
public class MyServerFeatures {
    @Tool(description = "回显字符串")
    public String echoString(@ToolArg(description = "输入字符串") String input) {
        return input;
    }

    @Prompt(description = "基础问候提示")
    public PromptMessage greeting(@PromptArg(description = "姓名") String name) {
        PromptMessage message = new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello " + name));
        return message;
    }
}
```

## 服务器功能管理

功能管理系统通过集中式的 `FeatureMgr` 类进行，负责包扫描、注解处理与功能存储。系统使用反射机制发现被注解的方法，并将功能元数据存储于并发哈希表中，确保运行时线程安全访问。

### 注解体系

注解体系围绕几个核心注解展开，用于标记类和方法以供 MCP 识别和暴露：

| 注解         | 目标      | 作用描述                      |
|--------------|-----------|-------------------------------|
| @McpService  | 类        | 标记服务发现类                |
| @Tool        | 方法      | 将方法暴露为 MCP 工具         |
| @ToolArg     | 参数      | 定义工具方法参数              |
| @Resource    | 方法      | 将方法暴露为 MCP 资源         |
| @Prompt      | 方法      | 将方法暴露为 MCP 提示         |
| @PromptArg   | 参数      | 定义提示方法参数              |

服务端配置通过 `FeatureMgr.init()` 注解驱动功能发现，自动扫描并注册含有 `@McpService` 注解且包含 `@Tool`、`@Resource`、`@Prompt` 方法的类。

### 初始化功能管理器

`FeatureMgr.init()` 方法负责整个注解发现流程。它首先扫描指定包下带有 `@McpService` 注解的类。

```java
FeatureMgr mgr = new FeatureMgr();
mgr.init("com.foo.myproduct");
```

## 服务器配置

包扫描初始化功能管理器后，可进行服务器配置，包括：

- 创建服务器实例并设置传输层
- 配置服务器名称与版本号
- 设置连接超时等参数

服务器配置由 `ServerConfig` 类管理，包含服务端元数据。初始化时还会进行协议版本协商，返回所支持的最高版本，或与客户端请求一致的版本。

```java
FeatureMgr mgr = new FeatureMgr();
mgr.init("com.foo.myproduct");

McpServer server = new McpServer();
server.setTransport(new ServerStdio(server));

ServerConfig serverConfig = new ServerConfig();
serverConfig.setName("MY_MCP_Server");
serverConfig.setVersion("1.0");
server.setServerConfig(serverConfig);
```