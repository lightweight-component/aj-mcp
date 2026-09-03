---
title:  提示（Prompt）使用
subTitle: 2024-12-05 by Frank Cheung
description:  提示（Prompt）使用
date: 2022-01-05
tags:
  - 提示
layout: layouts/docs-cn.njk
---

# 提示（Prompt）使用

MCP 的提示系统为定义、存储和检索用于大语言模型（LLM）的提示模板提供了标准化方式。MCP 的提示可以是静态（固定内容）或模板化（可自定义参数）的消息序列。

## 列出提示

列出所有可用提示：

```java
List<PromptItem> prompts = mcpClient.listPrompts();
assertEquals(5, prompts.size(), "期望返回 5 个提示");

PromptItem basic = findPromptByName("basic", prompts);
assertNotNull(basic, "提示 'basic' 不应为 null");
assertEquals("基础简单提示", basic.getDescription(), "提示 'basic' 的描述不符");
assertNull(basic.getArguments(), "'basic' 的参数应为空");

static PromptItem findPromptByName(String name, List<PromptItem> promptRefs) {
    for (PromptItem promptRef : promptRefs) {
        if (promptRef.getName().equals(name))
            return promptRef;
    }
    return null;
}
```

`listPrompts()` 请求并缓存第一页（默认页）。如需获取其他页，请调用 `listPrompts(int pageNo)`；各页缓存彼此独立。

``` java
List<PromptItem> prompts = mcpClient.listPrompts(1);
assertEquals(3, prompts.size(), "期望返回 3 个提示");
```

# 获取提示详情

获取某个提示的详细信息：

```java
GetPromptResult.PromptResultDetail prompt = mcpClient.getPrompt("basic", new HashMap<>());
assertEquals("基础简单提示", prompt.getDescription(), "描述不应为 null");
assertEquals(1, prompt.getMessages().size(), "期望仅有一条消息");

PromptMessage message = prompt.getMessages().get(0);
assertEquals(Role.USER, message.getRole(), "角色应为 USER");
assertEquals("Hello", ((ContentText) message.getContent()).getText(), "文本内容应为 'Hello'");
```

`getPrompt()` 方法返回 `PromptResultDetail`，包含提示的描述和消息内容。

通常，提示系统接收一个提示名称和参数映射。参数用于填充提示模板中的占位符。参数为 Map 类型，key 是参数名，value 是参数值；也可以传递
JSON 字符串。

如无需参数，则传递空 Map 即可。

## 嵌入式二进制资源

`ContentEmbeddedResource.Resource` 可以通过 `text` 表示文本内容，也可以通过 `blob` 表示 Base64 编码的二进制内容。还应设置
URI 和 MIME type，并且 `text` 与 `blob` 只设置其中一个。

```java
ContentEmbeddedResource.Resource resource = new ContentEmbeddedResource.Resource();
resource.setUri("file:///image.png");
resource.setMimeType("image/png");
resource.setBlob(base64Image);

ContentEmbeddedResource content = new ContentEmbeddedResource();
content.setResource(resource);
```
