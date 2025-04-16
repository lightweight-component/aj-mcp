---
title: Paging Query
subTitle: 2024-12-05 by Frank Cheung
description: This tutorial will guide you through the process of paging database data in SqlMan using a sample test code.
date: 2022-01-05
tags: paging query
layout: layouts/docs.njk
---
# Paging Query

Paging through large datasets is a common requirement in applications to enhance performance and provide a better user experience. This tutorial will guide you through the process of paging database data in SqlMan using a sample test code.

## Paging with Default Settings

The first paging example demonstrates how to retrieve paginated results with default settings: fecth the data between the first row to 9th row. We use the `page` method without any parameters:

```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM article").page();
```

## Paging with Custom Page Size and Number

The second example shows how to retrieve paginated results by specifying the record start and limit, like MySQL paging query:

```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM article").page(3, 5);
```

### Paging with Custom Class Mapping

The third example demonstrates how to map the results to a custom class (`Address`) and retrieve paginated results:
```java
PageResult<Address> result = new Sql(conn).input("SELECT * FROM shop_address").page(Address.class, 1, 2);
```

## Checking for Empty Results

The fourth example shows how to handle scenarios where the paginated result is empty:
```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address").page(Address.class, 100, 2);
assertEquals(0, result.size());
```

# Binding Parameters
Binding Parameters is also supported in paging query. The same way to do that.

```java
PageResult<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address where stat = ?", 1).page();
```

# Paging with Custom Database Vendor

The final example demonstrates how to set a custom database vendor (e.g., SQL Server) and retrieve paginated results:
```java
Sql sqlServer = new Sql(conn);
sqlServer.setDatabaseVendor(JdbcConstants.DatabaseVendor.SQL_SERVER);
PageResult<Map<String, Object>> result = sqlServer.input("SELECT * FROM article").page();
```

In this case, you cannot use method chaining anymore.

# JSQL Parser
We use the [JSQL Parser library](https://github.com/JSQLParser/JSqlParser) to implement paging functionality. It makes it easier to parse SQL statements and extract information from them. It also provides a convenient way to generate SQL statements dynamically.