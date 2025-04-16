---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---
# XML 中的 SQL
## 为什么要在 XML 中使用 SQL？

像著名的 MyBatis 框架一样，SQL 语句可以存储在 XML 文件中。这种方式有其特定的原因。以下是在 XML 中存储 SQL 的一些优点：

1. **关注点分离**：将 SQL 语句保存在 XML 文件中有助于将 SQL 逻辑与 Java 代码分离。这使代码库更加整洁，更易于管理。
1. **可读性**：当存储在 XML 中时，长 SQL 语句可以更具可读性。XML 文件的结构可以帮助使 SQL 查询更有条理，更易于理解。
1. **可维护性**：当 SQL 语句存储在 XML 中时，无需更改 Java 代码就可以更轻松地更新和维护它们。这对于具有大量 SQL 查询的大型项目特别有用。

SqlMan 在处理 SQL 语句方面采用了类似 MyBatis 的方法。

## 如何在 XML 中使用 SQL？
在开始之前，我们需要在 classpath `/sql` 目录下定义一个名为 `sql.xml` 的 XML 文件，其中包含 SQL 片段。

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

此方法通过标识符（`sqlId`）执行 XML 片段中定义的 SQL 语句，并返回包含结果的 DAO 对象。

```java
int result = new Sql(conn).inputXml("foo").queryOne(int.class); // 获取第一个结果
System.out.println(result);
assertTrue(result > 0);
```

现在我们已经了解了值和参数，可以将相同的知识应用到 XML 文件中的语句。

```java
int result;
result = new Sql(conn).inputXml("foo-2", 1).queryOne(int.class);
assertEquals(1, result);

result = new Sql(conn).inputXml("foo-3", mapOf("tableName", "shop_address", "stat", 1)).queryOne(int.class);
assertEquals(1, result);

result = new Sql(conn).inputXml("foo-4", mapOf("tableName", "shop_address", "abc", 2), 1).queryOne(int.class);
System.out.println(result); // TODO, 应该返回 0
```

此方法通过标识符（sqlId）执行 XML 片段中定义的 SQL 语句，并使用键值对和可变参数。

```java
/**
 * 执行 XML 片段中定义的 SQL 语句并返回 DAO 对象。
 *
 * @param sqlId     用于定位特定 SQL 语句的 SQL 标识符。
 * @param keyParams 用于替换 SQL 语句中变量的键值对参数。
 * @param params    用于替换 SQL 语句中占位符的可变参数。
 * @return 包含从数据库检索的数据的 DAO 对象。
 */
public DAO inputXml(String sqlId, Map<String, Object> keyParams, Object... params) {
    // 执行 SQL 并返回 DAO 对象的实现
}
```

## 标签支持
对于动态 SQL 生成，SqlMan 支持以下标签：`IF`。

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

`test`属性中的值是一个表达式，如`i > 10`。它可以是简单的布尔表达式，也可以是返回布尔值的更复杂表达式。任何符合 Spring Expression 模式的表达式都是有效的。

更多标签如`if...else`、`foreach`正在开发中。

## 调用 Java 方法
某些任务无法仅通过 SQL 完成。在这种情况下，最好调用 Java 方法来完成任务。SqlMan 支持使用语法`${T(com.ajaxjs.sqlman.sql.TestXml).w()}`在 SQL 中调用 Java 方法。

```xml
<sql id="foo-6">SELECT id FROM ${tableName} WHERE ${T(com.ajaxjs.sqlman.sql.TestXml).w()}</sql>
```