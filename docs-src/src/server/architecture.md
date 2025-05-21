---
title: Design Note
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - Design Note
layout: layouts/docs.njk
---
# Design Note

For Mcp server over SSE, there are two endpoints that should be known:

- SSE Url, this is the endpoint that the client will first connect to, which is at time of initialization. In this initialization, it'll return a POST Url(The second endpoint) form the server.
- POST Url, this is the real endpoint for the MCP business, client will send the request to this endpoint and the server will return the response by this endpoint. It's an SSE endpoint.
