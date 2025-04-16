---
title: 查询概念
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---
# 查询概念
## 开始前的概念

从数据库获取结果最直接的方式就是 SQL 查询。要使用 SqlMan Handle 执行查询，我们至少需要：

- 创建查询
- 选择如何传递查询参数
- 选择如何表示查询结果

让我们逐一来看这些要点。

## 创建查询
我们假设查询语句已经准备好执行，它是一个纯 SQL 语句。当然，数据库连接也已经建立。然后我们创建一个新的 `Sql` 实例，将 `conn` 传递给构造函数，并将 SQL 语句传递给 `input()` 方法。

```java
Map<String, Object> result = new Sql(conn).input("SELECT * FROM shop_address").query(); // fetch the first one
```
需要注意的是，`input()`方法接受 SQL 语句及其参数，但查询并不会立即执行，直到调用`query()`方法时才会执行。

目前我们还没有向查询传递任何参数。我们将在下一节讨论如何传递参数。

关于如何表示查询结果，可以分为两个部分：

- 需要获取多少结果？单个值？一行？还是列表？
- 用什么格式来表示结果？

SqlMan 对 JDBC ResultSet 进行了抽象封装，因为 ResultSet 的 API 使用起来比较繁琐。

因此，它提供了多种方式来访问查询或其他返回结果的语句所产生的列数据。

从返回值的结构来看，我们可以识别出几种类型：

- 单个元素，比如数字、字符串、日期等
- 单行数据库记录，其中字段和值以键值对的形式映射，对应 Java 中的 `Map`
- 多行记录，形成二维结构，对应 Java 中的 `List<Map>`

这些类型对应 SqlMan 中的以下方法：

- `queryOne(Type.class)` - 获取单个值（第一行第一列）。返回类型与传入的类型相匹配
- `query()` - 获取第一行数据，返回类型为 `Map<String, Object>`
- `queryList()` - 获取所有行的数据，即所有列的所有行，返回类型为 `List<Map<String, Object>>`

以下是完整示例：

```java
Map<String, Object> result = new Sql(conn).input("SELECT * FROM shop_address").query(); // fetch the first one
int result = new Sql(conn).input("SELECT COUNT(*) AS total FROM shop_address").queryOne(int.class); // fetch the first one
List<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address").queryList();
```

最后一个问题：用哪种格式来表示结果？

---- 我们现在将看到最简单的格式：Map（映射），在下一节中我们将介绍其他的格式。