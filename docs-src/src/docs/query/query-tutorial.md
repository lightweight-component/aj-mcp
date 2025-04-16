---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description:  How to page query in SqlMan
date: 2022-01-05
tags: Query
layout: layouts/docs.njk
---
# Query Tutorial
## Binding Parameters

Often, queries have a fixed portion and a parameterized portion. This has several advantages, including:

- security: by avoiding string concatenation, we prevent SQL injection
- ease: we don’t have to remember the exact syntax of complex data types such as timestamps
- performance: the static portion of the query can be parsed once and cached

SqlMan supports both positional and named parameters.

We insert positional parameters as question marks in a query or statement:

```java
Map<String, Object> result = new Sql(conn).input("SELECT * FROM shop_address WHERE id = ?", 1).query();
assertNotNull(result);
```
Just what we did in Classic JDBC query like parepred statement.

Named parameters, instead, start with `${` and are followed by a name, end with `}`:

```java
Map<String, Object> result；
result = new Sql(conn).input("SELECT * FROM ${tableName} WHERE id = #{stat}", mapOf("tableName", "shop_address", "stat", 1)).query();
assertNotNull(result);

result = new Sql(conn).input("SELECT * FROM ${tableName} WHERE id = ?", mapOf("tableName", "shop_address", "abc", 2), 1).query();
assertNotNull(result);
```
It allows to bind multiple named parameters together using a `Map` object.

Mixing `Map` object and the array of parameters is allowed, but `Map` should be the first parameter, and the rest are array of parameters.

## Return a Java Bean
Sometimes, we need to return a Java Bean instead of a `Map`. SqlMan provides a simple way to do this, just passing the Java Bean class as the query method as the parameter:

```java
Address result = new Sql(conn).input("SELECT * FROM shop_address").query(Address.class); 
List<Address> results = new Sql(conn).input("SELECT * FROM shop_address").queryList(Address.class);
```

Finally, we can map rows to a bean or some other custom class. 

