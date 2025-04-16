---
title: Setup SqlMan
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---
# 🔧 Setup SqlMan

## Requirements
### Java
SqlMan runs on Java 8+.

### Supported Databases

- MySQL
- PostgreSQL
- H2 Database
- Apache Derby

# Connecting to your database
Normally, you just tell Sqlman either a `DataSource` or a `Connection` to work. These're standard JDBC object that contain the connection information of database like URL, username, password, etc.

```java
Connection conn = JdbcConnection.getConnection("jdbc:mysql://localhost:3306/test", "root", "psw");
Map<String, Object> result = new Sql(conn).input("SELECT * FROM shop_address").query(); // fetch the first one
```

The code above is hardcoded, which is ONLY for demo/test purposes. In real world, you should use a connection pool to manage your connections.


## Config in Spring
 TODO