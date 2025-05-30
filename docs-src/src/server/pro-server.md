---
title: MCP Server SDK Prompts Development
subTitle: 2024-12-05 by Frank Cheung
description: MCP Server SDK Prompts Development
date: 2022-01-05
tags:
  - Prompts Development
layout: layouts/docs.njk
---

# Prompts Development

AJ-MCP uses annotations to define prompts within `@McpService` classes. The `@Prompt` annotation marks methods that serve as prompt handlers, while
`@PromptArg` defines parameters for interactive prompts.

Simple prompts return a single `PromptMessage` with text content. The `@Prompt` annotation requires a description and the method returns a configured
message with role and content.

```java
@Prompt(description = "Basic simple prompt")
public PromptMessage basic(){
        PromptMessage message=new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello"));

        return message;
        }
```

## Multi-Message Prompts

Prompts can return multiple messages by returning a `List<PromptMessage>`. This enables creating conversation-style prompts with multiple turns or
role-based interactions.

```java
@Prompt(description = "Prompt that returns two messages")
public List<PromptMessage> multi(){
        PromptMessage message1=new PromptMessage();
        message1.setRole(Role.USER);
        message1.setContent(new ContentText("first"));

        PromptMessage message2=new PromptMessage();
        message2.setRole(Role.USER);
        message2.setContent(new ContentText("second"));

        return Arrays.asList(message1,message2);
        }   
```

## Parameterized Prompts

Prompts can accept parameters using the `@PromptArg` annotation. Parameters become available to clients when listing prompts and are passed when
retrieving the prompt.

```java
@Prompt(description = "Parametrized prompt")
public PromptMessage parametrized(@PromptArg(description = "The name") String name){
        PromptMessage message=new PromptMessage();
        message.setRole(Role.USER);
        message.setContent(new ContentText("Hello "+name));

        return message;
        }
```

## Content Types and Rich Media

Prompts support multiple content types beyond text, including images and embedded resources. Each content type has specific properties and encoding
requirements.

| Content Type      | Class                   | Key Properties                  | Usage                  |
|-------------------|-------------------------|---------------------------------|------------------------|
| Text              | ContentText             | text                            | Simple text messages   |
| Image             | ContentImage            | data, mimeType                  | Base64-encoded images  |
| Embedded Resource | ContentEmbeddedResource | resource.uri, resource.mimeType | Binary data references |

## Protocol Data Structures

The prompt system uses specific protocol structures for communication between client and server.

| Class           | Purpose                     | Key Fields                          |
|-----------------|-----------------------------|-------------------------------------|
| PromptItem      | Prompt metadata for listing | name, description, arguments        |
| PromptMessage   | Individual message content  | role, content                       |
| GetPromptResult | Response wrapper            | result.description, result.messages |
| PromptArgument  | Parameter definition        | name, description, required         |

## Role Enumeration

The Role enum defines message roles in prompt conversations:

- USER - Messages from the user perspective
- ASSISTANT - Messages from the AI assistant perspective
- SYSTEM - System-level instructions or context
