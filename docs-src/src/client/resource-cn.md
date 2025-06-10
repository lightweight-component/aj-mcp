---
title: 资源使用
subTitle: 2024-12-05 by Frank Cheung
description: 资源使用
date: 2022-01-05
tags:
  - 资源使用
layout: layouts/docs-cn.njk
---
## 资源（Resource）使用

资源系统使 LLM 驱动的应用能够通过统一接口访问结构化内容。每个资源通过 URI 标识，可以表示任何对 LLM 有用的内容类型。AJ MCP 客户端提供发现和访问服务器端资源的方法。

### 列出资源

列出所有可用资源：

```java
List<ResourceItem> resourceList = mcpClient.listResources();
assertEquals(2, resourceList.size(), "期望恰好有两个资源");

ResourceItem blob = getResourceRef("blob", resourceList);
assertNotNull(blob, "资源 'blob' 不应为 null");
assertEquals("blob", blob.getName(), "资源名不符（'blob'）");
assertEquals("file:///blob", blob.getUri(), "URI 不符（'blob'）");
assertEquals("image/jpg", blob.getMimeType(), "MIME 类型不符（'blob'）");
assertEquals("A nice pic", blob.getDescription(), "描述不符（'blob'）");
```


此方法将跨多个页面获取所有的资源，并没有分页。如果你需要对分页进行更精细的控制，可以改用重载的`listResources(int pageNo)`方法。

``` java
List<ResourceItem> resourceList = mcpClient.listResources(1);
assertEquals(2, resourceList.size(), "Expected exactly two resources");
```

### 读取资源内容

读取某个资源内容：

```java
GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///text");
assertEquals(1, response.getContents().size(), "期望内容数量为 1");

ResourceContent contents = response.getContents().get(0);

ResourceContentText textContents = (ResourceContentText) contents;
assertEquals("file:///text", textContents.getUri(), "URI 应为 'file:///text'");
assertEquals("text888", textContents.getText(), "文本内容应为 'text888'");
```

`readResource()` 方法返回 `ResourceResultDetail`，其中包含资源内容数组。每项内容类型根据 MIME 类型决定，可能是文本内容，也可能是二进制内容。

```java
GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///blob");
assertEquals(1, response.getContents().size(), "期望内容数量为 1");

ResourceContent contents = response.getContents().get(0);

ResourceContentBinary blobContents = (ResourceContentBinary) contents;
assertEquals("file:///blob", blobContents.getUri(), "URI 应为 'file:///blob'");
assertEquals("blob", blobContents.getBlob(), "二进制内容应为 'blob'");
```

你也可以通过模板资源读取内容，即可传递参数给资源：

```java
GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///text-template/hello");
assertEquals(1, response.getContents().size(), "期望内容数量为 1");

ResourceContent contents = response.getContents().get(0);

ResourceContentText textContents = (ResourceContentText) contents;
assertEquals("file:///text-template/hello", textContents.getUri(), "URI 应为 'file:///text-template/hello'");
assertEquals("text hello", textContents.getText(), "文本内容应为 'text hello'");
```

<!--
### 资源订阅

客户端可订阅资源变更通知，以便在资源更新时及时获知：

err := client.Subscribe(ctx, mcp.SubscribeRequest{
    Params: mcp.SubscribeParams{
        URI: "test://resource/path",
    },
})

如需取消资源通知：

err := client.Unsubscribe(ctx, mcp.UnsubscribeRequest{
    Params: mcp.UnsubscribeParams{
        URI: "test://resource/path",
    },
})

-->