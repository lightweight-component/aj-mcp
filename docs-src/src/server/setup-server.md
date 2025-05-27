---
title: MCP Client SDK Setup
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - client setup
layout: layouts/docs.njk
---

# MCP Client SDK Setup

## Install Dependency

We’ll need the AJ MCP SDK for making API requests. Install them with:

```xml

<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>aj-mcp-server</artifactId>
    <version>1.1</version>
</dependency>
```

We can find the latest version:
[![Maven Central](https://img.shields.io/maven-central/v/com.ajaxjs/aj-mcp-client?label=Latest%20Release)](https://central.sonatype.com/artifact/com.ajaxjs/aj-mcp-client)

## Setup the Transport

First, we need to create the transport. There are two types of transport you can choose from for your application, depending on the type of your MCP
server.

### Stdio Transport

'Stdio' stands for Standard Input/Output, by using command line to interact between the programme and human in short. But here is between the MCP
Client
and the MCP Server. Usually, we use stdio for the local application, such as a `*.exe` programme or a Java Jar programme, and so on.

``` java
// The MCP server is a Java programme, runs on stdio.
McpTransport transport = StdioTransport.builder()
    .command(Arrays.asList("java", "-jar", "C:\\app\\my-app-jar-with-dependencies.jar"))
    .logEvents(true)
    .build();
```

Let's take a look at a `.exe` programme as an example:

``` java
// The MCP server is an executable programme, runs on stdio.
McpTransport transport = StdioTransport.builder()
.command("C:\\app\\my-app.exe", "-token", "dd4df2sx32ds"))
.logEvents(true)
.build();
```

If you want to check out the fully logs of the client, you can set `logEvents` to `true`. This is good for debugging or learning the MCP protocol, to get more
about the JSON messages of this protocol.

## Configure SDK

| Property        | Note                                                                                                                                                                              | Type of value | Example of value         |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|--------------------------|
| clientName      | Sets the name that the client will use to identify itself to the MCP server in the initialization message.                                                                        | String        | myapp/foo-app            |
| clientVersion   | Sets the version string that the client will use to identify itself to the MCP server in the initialization message. The default value is "1.0".                                  | String        | 1.0/2.1.2                |
| protocolVersion | Sets the protocol version that the client will advertise in the initialization message. The default value right now is "2024-11-05", but will change over time in later versions. | String        | 2024-11-05               |
| requestTimeout  | Sets the timeout for tool execution. This value applies to each tool execution individually. The default value is 60 seconds. A value of zero means no timeout.                   | Duration      | `Duration.ofSeconds(60)` |

<style>
table th:nth-child(2) {
 min-width: 400px;
}
table th:nth-child(3), table td:nth-child(3) {
 min-width: 120px!important;
 width: 120px;
}

table td:nth-child(2) {
 text-align: left;
}
</style>
