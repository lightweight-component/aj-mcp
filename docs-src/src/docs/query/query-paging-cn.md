---
title: 分页查询
subTitle: 2024-12-05 by Frank Cheung
description: 本教程将通过示例代码指导您如何在 SqlMan 中实现数据库数据的分页。
date: 2022-01-05
tags: 分页
layout: layouts/docs-cn.njk
---
# 分页查询

在应用程序中，对大型数据集进行分页是一个常见需求，这可以提高性能并提供更好的用户体验。本教程将通过示例代码指导您如何在 SqlMan 中实现数据库数据的分页。

## 使用默认设置的分页

第一个分页示例演示如何使用默认设置获取分页结果：获取第 1 行到第 9 行之间的数据。我们不带任何参数使用 `page` 方法：

```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM article").page();
```

## 使用自定义页面大小和页码的分页
第二个示例展示如何通过指定记录起始位置和 limit大小来获取分页结果，如同 MySQL 的 LIMIT 3,5 分页方式：

```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM article").page(3, 5);
 ```

## 使用自定义类映射的分页
第三个示例演示如何将结果映射到自定义类（Address）并获取分页结果：

```java
PageResult<Address> result = new Sql(conn).input("SELECT * FROM shop_address").page(Address.class, 1, 2);
```

## 检查空结果
第四个示例展示如何处理分页结果为空的情况：

```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address").page(Address.class, 100, 2);
assertEquals(0, result.size());
```

## 参数绑定
分页查询同样支持参数绑定。使用方式与普通查询相同：

```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address where stat = ?", 1).page();
```

## 使用自定义数据库供应商的分页
最后一个示例演示如何设置自定义数据库供应商（例如 SQL Server）并获取分页结果：

```java
Sql sqlServer = new Sql(conn);
sqlServer.setDatabaseVendor(JdbcConstants.DatabaseVendor.SQL_SERVER);
PageResult<Map<String, Object>> result = sqlServer.input("SELECT * FROM article").page();
```

在这种情况下，您将不能使用方法链式调用。

## JSQLParser
我们使用 JSQLParser 库 来实现分页功能。它使解析 SQL 语句和提取信息变得更容易，同时还提供了一种动态生成 SQL 语句的便捷方式。