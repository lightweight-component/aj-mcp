---
title: Quick Start
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---
# 快速开始
## 🔧 安装 SqlMan
首先，我们需要在项目依赖中添加 SqlMan 模块：

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>sqlman</artifactId>
    <version>1.3</version>
</dependency>
```

在本文中，我们将使用 HSQL 数据库作为示例：
```xml
<dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <version>2.2.220</version>
</dependency>
```
你可以在 [Maven Central](https://central.sonatype.com/artifact/com.ajaxjs/sqlman) 上找到 SqlMan 的最新版本。

接下来，我们将编写如下 Java 代码：

```java
public static void main(String[] args) throws SQLException {
    // 配置 H2 数据源
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("password");

    // 获取数据库连接
    Connection conn = dataSource.getConnection();

    try (Statement stmt = conn.createStatement()) {
        // 创建商店地址表
        stmt.execute("CREATE TABLE shop_address (\n" +
                "    id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                "    name VARCHAR(255) NOT NULL,\n" +
                "    address VARCHAR(255) NOT NULL,\n" +
                "    phone VARCHAR(20),\n" +
                "    receiver VARCHAR(255),\n" +
                "    stat INT,\n" +
                "    create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n" +
                ");");

        // 插入示例数据
        stmt.execute("INSERT INTO shop_address (name, address, phone, receiver, stat)\n" +
                "VALUES\n" +
                "('商店 A', '主街 123 号', '123-456-7890', '张三', 0),\n" +
                "('商店 B', '榆树街 456 号', '234-567-8901', '李四', 0),\n" +
                "('商店 C', '橡树街 789 号', '345-678-9012', '王五', 0),\n" +
                "('商店 D', '枫树街 101 号', '456-789-0123', '赵六', 1),\n" +
                "('商店 E', '桦树街 202 号', '567-890-1234', '钱七', 1);");
    }

    // 查询所有记录并打印
    List<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address").queryList();
    System.out.println(result);
    conn.close();
}
```