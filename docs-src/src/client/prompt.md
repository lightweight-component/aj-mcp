---
title:  Working with Prompts
subTitle: 2024-12-05 by Frank Cheung
description:  Working with Prompts
date: 2022-01-05
tags:
  - Prompts
layout: layouts/docs.njk
---

# Working with Prompts
The prompt system in MCP provides a standardized way to define, store, and retrieve prompt templates for use with Large Language Models (LLMs). 
Prompts in MCP are predefined message sequences that can be either static (with fixed content) or templated (with customizable parameters).

## Listing Prompts

To list available prompts:

```java
List<PromptItem> prompts = mcpClient.listPrompts();
assertEquals(5, prompts.size(), "Expected 5 prompts");

PromptItem basic = findPromptByName("basic", prompts);
assertNotNull(basic, "Prompt 'basic' should not be null");
assertEquals("Basic simple prompt", basic.getDescription(), "Description mismatch for 'basic'");
assertNull(basic.getArguments(), "'basic' arguments should be empty");

static PromptItem findPromptByName(String name, List<PromptItem> promptRefs) {
        for (PromptItem promptRef : promptRefs) {
            if (promptRef.getName().equals(name))
                return promptRef;
        }

    return null;
}
```

`listPrompts()` requests and caches the first/default page. Use `listPrompts(int pageNo)` for another page. Each page is cached independently.

``` java
List<PromptItem> prompts = mcpClient.listPrompts(1);
assertEquals(3, prompts.size(), "Expected 3 prompts");
```

# Getting Prompt Details

To get details of a specific prompt:

```java
GetPromptResult.PromptResultDetail prompt = mcpClient.getPrompt("basic", new HashMap<>());
assertEquals("Basic simple prompt", prompt.getDescription(), "Description should not be null");
assertEquals(1, prompt.getMessages().size(), "Expected exactly one message");

PromptMessage message = prompt.getMessages().get(0);
assertEquals(Role.USER, message.getRole(), "Role should be USER");
assertEquals("Hello", ((ContentText) message.getContent()).getText(), "Text content should be 'Hello'");
```

The `getPrompt()` method returns a `PromptResultDetail` containing the prompt's description and messages.

Normally, the prompt system receives a prompt name and a map of arguments. The arguments are used to fill in the placeholders in the prompt template.
Arguments may be supplied as a map, where each key is an argument name, or as a JSON object string.

If you don't want to use arguments, you can pass an empty map.
