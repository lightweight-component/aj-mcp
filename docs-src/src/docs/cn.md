---
title: Docs Home
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---
# 欢迎来到 SqlMan 文档中心

这是一个用于关系型数据库（RDBMS）数据操作的 Java 辅助库，涵盖从底层 SQL 操作到高级低代码快速开发集成。首先，它封装了常见的 JDBC 操作，同时保留原始 SQL 语句输入（SQL 可以存储在 XML 或 Java 注解中）；其次，它为 Java 实体提供便捷的 CRUD（创建、读取、更新、删除）操作，无需编写 SQL；最后，它提供了一个集成前两者特性的快速开发平台，只需提供 SQL 语句即可生成 API 接口，使用更加便捷。

### 使用 SqlMan 的一些优势：

- 熟悉的使用方式：
    - `sql().input('SELECT * FROM table WHERE id=?', 1).query()` -- 高度兼容 Spring JdbcTemplate/Apache DBUtils。
    - 在 XML 中编写多行 SQL -- 高度兼容 MyBatis Mapper。
    - `@Insert`、`@Update`、`@Delete`、`@Query` 注解 -- 高度兼容 JPA。
    - 自动将 ResultSet 填充到 JavaBean 属性中。无需手动调用 setter 方法将列值复制到 bean 实例中。ResultSet 的每一行都可以由一个完全填充的 bean 实例表示。此外，Map 类型是可选的。
- 核心理念围绕 SQL。增强的 SQL 逻辑，支持动态 SQL（IF/ForEach）。
- 内置常用的 CRUD/分页 功能，使简单系统可以实现 0% SQL 编写，在复杂项目中可节省 50% 的工作量。
- 提供丰富的扩展功能，80% 的功能都可以自定义和扩展。

### SqlMan 的设计理念：
- 小巧 - 近乎偏执的简单哲学。您应该能在短时间内理解整个包的内容。
- 透明 - SqlMan 不会在后台进行任何魔法操作。您提供查询，它执行并为您清理。
- 快速 - 您无需创建大量临时对象就能使用 SqlMan。

### SqlMan 不是：

- 一个对象/关系映射桥接器 - 已经有很多优秀的 O/R 工具了。SqlMan 是为那些想要使用 JDBC 但不想处理繁琐细节的开发者设计的。
- 不提供函数式链式调用，因为作者认为这样的可读性较差且调试不便。
- 任何形式的重量级框架 - 这里的目标是成为一个直接且易用的 JDBC 辅助库。

## 源代码
基于 GNU GENERAL PUBLIC LICENSE v3.0 许可。
 
- Github：[https://github.com/lightweight-component/SqlMan](https://github.com/lightweight-component/SqlMan)
- Gitcode：[https://gitcode.com/lightweight-component/SqlMan](https://gitcode.com/lightweight-component/SqlMan)，供中国用户更快访问。

## 链接

[网站](https://sqlman.ajaxjs.com) | [文档](https://sqlman.ajaxjs.com/docs) | [JavaDoc](https://github.com/lightweight-component/SqlMan)