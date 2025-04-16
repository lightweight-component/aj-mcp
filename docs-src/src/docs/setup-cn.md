---
title: Setup SqlMan
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---

# 🔧 配置 SqlMan

## 系统要求
### Java
SqlMan 运行需要 Java 8 或更高版本。

### 支持的数据库

- MySQL
- PostgreSQL
- H2 数据库
- Apache Derby

# 连接数据库
通常情况下，你只需要为 SqlMan 提供一个 `DataSource` 或 `Connection` 对象即可工作。这些是标准的 JDBC 对象，包含了数据库连接所需的信息，如 URL、用户名、密码等。

```java
Connection conn = JdbcConnection.getConnection("jdbc:mysql://localhost:3306/test", "root", "psw");
Map<String, Object> result = new Sql(conn).input("SELECT * FROM shop_address").query(); // 获取第一条记录
```

上面的代码是硬编码的，仅用于演示/测试目的。在实际应用中，你应该使用连接池来管理数据库连接。

## Spring 框架中的配置
待完善