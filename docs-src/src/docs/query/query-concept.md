---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---
# Query Concept
## The Concept before start

The most straightforward expression that produces results from the DB is a SQL query.To issue a query with a SqlMan Handle, we have to, at least:

- create the query, 
- choose how to pass parameters to the query
- choose how to represent the result(s), after the query is executed

We’ll now look at each of the points above.

## Create the query
We assume that the query is ready to be executed, it's pure SQL statement. Of course, the connection is already established. Then we create a new `Sql` instance, pass `conn` to the constructor and SQL statement to `input()` method.

```java
Map<String, Object> result = new Sql(conn).input("SELECT * FROM shop_address").query(); // fetch the first one
```

It's important to note that `input()` method accepts SQL statement and its parameters. The query is not executed until the `query()` method is called.

Currently we didn't pass any parameters to the query. We'll talk about how to pass parameters in the next section.

Here is a question about how to represent the result(s), it can be divded into two parts: 

- How many results to fetch? a single one? or a row? or list?
- Which format to represent the result(s)?

SQlMan abstracts away from the JDBC ResultSet, which has a quite cumbersome API.

Therefore, it offers several possibilities to access the columns resulting from a query or some other statement that returns a result. 

From the return structure of value(s), we can identify several types:

- A single element, such as a number, a string, a date, etc.
- A single database row, where fields are mapped to the values like key/value pairs, corresponding to `Map` in Java.
- Multiple rows, which form a two-dimensional structure, corresponding to `List<Map>` in Java.

These types correspond to the following methods in SqlMan:


- `queryOne(Type.class)`- Fetches the single value, means the first column of the first row. The return type matches what you pass in
- `query()`- Fetches the first row, returning a `Map<String, Object>`.
- `queryList()`- Fetch the list of values, means all the columns of all the rows, returning a `List<Map<String, Object>>`.

Here are complete examples:

```java
Map<String, Object> result = new Sql(conn).input("SELECT * FROM shop_address").query(); // fetch the first one
int result = new Sql(conn).input("SELECT COUNT(*) AS total FROM shop_address").queryOne(int.class); // fetch the first one
List<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address").queryList();
```
## Return Format
There is the last question: Which format to represent the result(s)?

---- We’ll now see the simplest ones: Map, we’ll see the more complex ones in the next section.