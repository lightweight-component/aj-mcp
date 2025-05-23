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

For Mcp server over the SSE, there are two types of endpoint that should be known:

- SSE Url, this is the endpoint that the client will first connect to, which is at time of initialization. In this initialization, it'll return a POST Url(The second endpoint) form the server. It's an endpoint which is
  keep-alive, implemented the protocol of SEE.
- POST Url, this is NOT an SSE endpoint, it's just a regular endpoint that only receives POST request from the client. Client will send the request to this endpoint, BUT the server will return the response by the SSE
  endpoint, NOT this endpoint(This endpoint may return nothing).

In simple words, The POST endpoint is to 'Writing Data', and the SSE endpoint is to 'Returning Data'. The SSE endpoint is the endpoint that the server will return the response to, and the POST endpoint is the endpoint that the
client will send the request to. SSE endpoint is a long time connection, the client is listening to this endpoint, so that it can receive the response from the server all the time.