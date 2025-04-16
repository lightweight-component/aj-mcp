---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---
# SQL in XML
## Why SQL in XML?

Like the famous framework MyBatis, SQL statements can be stored in XML files. There are several reasons for this approach. Here are some advantages of storing SQL in XML:

1. **Separation of Concerns**: Keeping SQL statements in XML files helps separate the SQL logic from the Java code. This makes the codebase cleaner and easier to manage.
1. **Readability**: Long SQL statements can be more readable when stored in XML. The structure of the XML file can help make the SQL queries more organized and easier to understand.
1. **Maintainability**: When SQL statements are stored in XML, it is easier to update and maintain them without changing the Java code. This is particularly useful for large projects with many SQL queries.

SqlMan follows a similar approach to MyBatis in handling SQL statements.


## How to use SQL in XML?
Before we begin, we should have the SQL fragments defined in an XML file, named `sql.xml`, stored in the classpath `/sql`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <sql id="foo">SELECT COUNT(*) AS total FROM shop_address</sql>
    <sql id="foo-2">SELECT COUNT(*) AS total FROM shop_address WHERE id = ?</sql>
    <sql id="foo-3">SELECT id FROM ${tableName} WHERE id = #{stat}</sql>
    <sql id="foo-4">SELECT * FROM ${tableName} WHERE id = ?</sql>
    <sql id="foo-5">
        <if test="type=='address'">
            SELECT COUNT(*) AS total FROM shop_address
        </if>
        <if test="type=='article'">
            SELECT COUNT(*) AS total FROM ${tableName}
        </if>
    </sql>
    <sql id="foo-6">SELECT id FROM ${tableName} WHERE ${T(com.ajaxjs.sqlman.sql.TestXml).w()}</sql>
</root>
```

This method executes an SQL statement defined in an XML fragment by its identifier (`sqlId`) and returns a DAO object containing the result.

```java
int result = new Sql(conn).inputXml("foo").queryOne(int.class); // fetch the first one
System.out.println(result);
assertTrue(result > 0);
```

Now that we’ve seen before, values, and parameters, we can go back to statements in XML file and apply the same knowledge.

```java
int result;
result = new Sql(conn).inputXml("foo-2", 1).queryOne(int.class);
assertEquals(1, result);

result = new Sql(conn).inputXml("foo-3", mapOf("tableName", "shop_address", "stat", 1)).queryOne(int.class);
assertEquals(1, result);

result = new Sql(conn).inputXml("foo-4", mapOf("tableName", "shop_address", "abc", 2), 1).queryOne(int.class);
System.out.println(result); // TODO, should be return 0
```
This method executes an SQL statement defined in an XML fragment by its identifier (`sqlId`) with key-value pairs and variable parameters.

```java
/**
 * Executes an SQL statement defined in an XML fragment and returns a DAO object.
 *
 * @param sqlId     SQL identifier used to locate the specific SQL statement.
 * @param keyParams Key-value pair parameters used to replace variables in the SQL statement.
 * @param params    Variable parameters used to replace placeholders in the SQL statement.
 * @return A DAO object containing the data retrieved from the database.
 */
public DAO inputXml(String sqlId, Map<String, Object> keyParams, Object... params) {
    // Implementation to execute the SQL and return DAO object
}
```

## Tag Support
For dynamic SQL generation, SqlMan supports the following tags:`IF`.

```xml
<sql id="foo-5">
    <if test="type=='address'">
        SELECT COUNT(*) AS total FROM shop_address
    </if>
    <if test="type=='article'">
        SELECT COUNT(*) AS total FROM ${tableName}
    </if>
</sql>
```

The value in `test` attribute is a expression, like `i > 10`. It can be a simple boolean expression or a more complex expression that returns a boolean value. Any expression that matches the pattern of Spring Expression is illegal.

More tags like `if...else`, `foreach` are on the way.

## Calling Java Methods

Some tasks cloud not be accomplished by SQL alone. In such cases, it's better to call Java methods to do the job. SqlMan supports calling Java methods in SQL using the syntax `${T(com.ajaxjs.sqlman.sql.TestXml).w()}`.

```xml
<sql id="foo-6">SELECT id FROM ${tableName} WHERE ${T(com.ajaxjs.sqlman.sql.TestXml).w()}</sql>
```