---
title: Working with Resources
subTitle: 2024-12-05 by Frank Cheung
description: Working with Resources
date: 2022-01-05
tags:
  - Resources
layout: layouts/docs.njk
---

## Working with Resources

The resource system enables LLM-powered applications to access structured content through a unified interface.
Resources are identified by URIs and can represent any type of content that might be useful for an LLM to process.
The AJ MCP client provides methods for discovering and accessing resources exposed by the server.

### Listing Resources

To list available resources:

``` java
List<ResourceItem> resourceList = mcpClient.listResources();
assertEquals(2, resourceList.size(), "Expected exactly two resources");

ResourceItem blob = getResourceRef("blob", resourceList);
assertNotNull(blob, "Resource 'blob' should not be null");
assertEquals("blob", blob.getName(), "Name mismatch for 'blob'");
assertEquals("file:///blob", blob.getUri(), "URI mismatch for 'blob'");
assertEquals("image/jpg", blob.getMimeType(), "MIME type mismatch for 'blob'");
assertEquals("A nice pic", blob.getDescription(), "Description mismatch for 'blob'");
```

`listResources()` requests and caches the first/default page. Use `listResources(int pageNo)` for another page. Each
page is cached independently, so requesting page 2 does not overwrite page 0.

``` java
List<ResourceItem> resourceList = mcpClient.listResources(1);
assertEquals(2, resourceList.size(), "Expected exactly two resources");
```

### Reading Resource Contents

To read the contents of a resource:

``` java
GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///text");
assertEquals(1, response.getContents().size(), "Expected exactly one content");

ResourceContent contents = response.getContents().get(0);

ResourceContentText textContents = (ResourceContentText) contents;
assertEquals("file:///text", textContents.getUri(), "URI should be 'file:///text'");
assertEquals("text888", textContents.getText(), "Text content should be 'text'");

```

The `readResource()` method returns a `ResourceResultDetail` containing an array of resource contents.
Each resource content can be either text content or binary content, depending on the resource's MIME type.

``` java
GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///blob");
assertEquals(1, response.getContents().size(), "Expected exactly one content");

ResourceContent contents = response.getContents().get(0);

ResourceContentBinary blobContents = (ResourceContentBinary) contents;
assertEquals("file:///blob", blobContents.getUri(), "URI should be 'file:///blob'");
assertEquals("blob", blobContents.getBlob(), "Blob content should be 'blob'");
```

Resource templates registered by the server can be listed and read through their expanded URI. For interoperable
pagination, prefer `listResourceTemplatePage(cursor)` and pass the returned opaque `nextCursor` unchanged.

``` java
GetResourceResult.ResourceResultDetail response = mcpClient.readResource("file:///text-template/hello");
assertEquals(1, response.getContents().size(), "Expected exactly one content");

ResourceContent contents = response.getContents().get(0);

ResourceContentText textContents = (ResourceContentText) contents;
assertEquals("file:///text-template/hello", textContents.getUri(), "URI should be 'file:///text-template/hello'");
assertEquals("text hello", textContents.getText(), "Text content should be 'text hello'");
```

### Resource Subscriptions

The client can subscribe to resource changes to receive notifications when resources are updated:

mcpClient.subscribeResource("test://resource/path");

To stop receiving notifications for a resource:

mcpClient.unsubscribeResource("test://resource/path");
