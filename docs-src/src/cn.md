---
title: Docs Home
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---
# Welcome to AJ-MCP's Docs Center

This is a Java helper library for data manipulation of RDBMS (Databases), from low-level SQL operations to advanced low-code rapid development integration. First of all, it encapsulates common JDBC operations while retaining the original SQL statement input (SQL can be stored in XML or Java annotations); Secondly, it provides convenient CRUD (Create, Read, Update, Delete) operations for Java entities, eliminating the need to write SQL; Finally, it offers a rapid development platform that integrates the features of the former two, allowing the generation of API interfaces with just SQL statements, making it more user-friendly.

<!-- 
封装了 JDBC 常见的操作，保留原始的 SQL 语句输入（SQL 可以保存在 XML 或者 Java 注解）；其次，针对 Java 实体提供便捷的增删改查操作，这时基本不用写 SQL；最后，提供一个快速开发平台，集成前二者的特色，只须提供 SQL 语句即可生成 API 接口，更易用。
简化 JDBC 调用处理，抽取出重复代码，以便开发人员只专注于与数据库相关的操作：查询与更新。 -->

### Some of the advantages of using SqlMan are:

- Familiar way for you:
    - `sql().input('SELECT * FROM table WHERE id=?', 1).query()` -- Highly compatible with Spring JdbcTemplate/Apache DBUtils.
    - Multiple lines of SQL written in XML -- Highly compatible with MyBatis Mapper.
    - `@Insert`、`@Update`、`@Delete`、`@Query` annotations -- Highly compatible with JPA.
    - Automatically populate JavaBean properties from ResultSets. You don't need to manually copy column values into bean instances by calling setter methods. Each row of the ResultSet can be represented by one fully populated bean instance. Besides, Map type is optional.
- Core philosophy centers around SQL. Enhanced SQL logic, supporting dynamic SQL (IF/ForEach).
- Built-in common CRUD//Paging functionality, allowing simple systems to write 0% SQL, and saving 50% of the workload in complex projects.
- Offers a rich set of extension features, with 80% of the functionality being customizable and expandable.
<!-- - LowCode Platform ready for advanced integration,  [check it out!](low-code) -->

### SqlMan is designed to be:
<!-- 近乎偏执的简单哲学 -->
- Small - Almost obsessively simple philosophy. You should be able to understand the whole package in a short amount of time.
- Transparent - SqlMan doesn't do any magic behind the scenes. You give it a query, it executes it and cleans up for you.
- Fast - You don't need to create a million temporary objects to work with SqlMan.


<!-- 设计原则

    小 - SqlMan 库的体积很小，只有较少的类，因此易于理解和使用。
    透明 - SqlMan 库在后台没有做太多工作，它只需查询并执行。
    快速 - SqlMan 库类不会创建许多背景对象，并且在数据库操作执行中速度非常快。 -->

### SqlMan is NOT:

- An Object/Relational bridge - there are plenty of good O/R tools already. SqlMan is for developers looking to use JDBC without all the mundane pieces.
- Does not provide a functional-style chained call, as the author believes that readability is poor and debugging is inconvenient.
<!--     A Data Access Object (DAO) framework - SqlMan can be used to build a DAO framework though.
    An object oriented abstraction of general database objects like a Table, Column, or PrimaryKey. -->
- A heavyweight framework of any kind - the goal here is to be a straightforward and easy to use JDBC helper library.


## Source Code
Under GNU GENERAL PUBLIC LICENSE v3.0.
 
- Github: [https://github.com/lightweight-component/SqlMan](https://github.com/lightweight-component/SqlMan)
- Gitcode: [https://gitcode.com/lightweight-component/SqlMan](https://gitcode.com/lightweight-component/SqlMan), for Chinese users faster access.


## Links

[Website](https://sqlman.ajaxjs.com) | [Documents](https://sqlman.ajaxjs.com/docs)  | [JavaDoc](https://github.com/lightweight-component/SqlMan)