---
title: Quick Start
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---
# Quick Start
## 🔧 Install SqlMan
To get started, we just have to include the one SqlMan module in our dependencies:

```xml
<dependency>
    <groupId>com.ajaxjs</groupId>
    <artifactId>sqlman</artifactId>
    <version>1.3</version>
</dependency>
```

Over the course of this article, we’ll show examples using the HSQL database:
```xml
<dependency>
    <groupId>org.hsqldb</groupId>
    <artifactId>hsqldb</artifactId>
    <version>2.2.220</version>
</dependency>
```
We can find the latest version of SqlMan on [Maven Central](https://central.sonatype.com/artifact/com.ajaxjs/sqlman).

<!-- > About Java Version
>
> Currently, SqlMan only supports **Java 11** and above. However, we are aware that there is a significant user base still using JDK 8. Should the need arise, we are committed to maintaining compatibility with Java 8 by making a few modifications to the code. -->

Next we going to write Java code as follows:

```java
public static void main(String[] args) throws SQLException {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("password");

    Connection conn = dataSource.getConnection();

    try (Statement stmt = conn.createStatement()) {
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

        stmt.execute("INSERT INTO shop_address (name, address, phone, receiver, stat)\n" +
                "VALUES\n" +
                "('Shop A', '123 Main St', '123-456-7890', 'John Doe', 0),\n" +
                "('Shop B', '456 Elm St', '234-567-8901', 'Jane Smith',0),\n" +
                "('Shop C', '789 Oak St', '345-678-9012', 'Alice Johnson', 0),\n" +
                "('Shop D', '101 Maple St', '456-789-0123', 'Bob Brown', 1),\n" +
                "('Shop E', '202 Birch St', '567-890-1234', 'Charlie Davis', 1);");
    }

    List<Map<String, Object>> result = new Sql(conn).input("SELECT * FROM shop_address").queryList();
    System.out.println(result);
    conn.close();
}
```