---
title: Architecture
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - Architecture
layout: layouts/docs.njk
---
# MCP Client and Server Architecture
The AJ MCP SDK is structured around several core components that work together to implement the MCP specification:

![](/asset/imgs/a-1.jpg)

The architecture follows a client-server model with flexible transport options. The client interacts with the server using JSON-RPC over either HTTP (with Server-Sent Events) or standard I/O pipes.