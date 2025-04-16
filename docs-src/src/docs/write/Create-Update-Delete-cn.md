---
title: 数据库数据的更新、创建和删除
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---

# 数据库数据的更新、创建和删除
本教程将指导您如何使用 SqlMan 在数据库中更新、创建和删除数据。我们将使用示例测试代码来演示如何执行这些操作。

## 创建记录

`testCreate`方法演示了如何向`shop_address`表中插入新记录：

```java
@Test
public void testCreate() {
    // Insert a new record into the shop_address table
    String sql = "INSERT INTO shop_address (name, address, phone, receiver) " +
            "VALUES ('Home', 'Tree Road', '3412', 'Jack')";
    CreateResult<Integer> result;
    result = new Sql(conn).input(sql).create(true, Integer.class);
    assertTrue(result.isOk());

    // Insert a new record with parameters
    sql = "INSERT INTO shop_address (name, address, phone, receiver) " +
                "VALUES (${name}, ?, '3412', ?)";
        
    // mixing parameters with Map and Array
    result = new Sql(conn).input(sql, mapOf("name", "'office'"),"Kid Place", "Tom").create(true, Integer.class);
    assertTrue(result.isOk());

    Address address = new Sql(conn).input("SELECT * FROM shop_address WHERE id = ?", result.getNewlyId()).query(Address.class);
    System.out.println(address);
}
```

在`input()`方法中，同时支持位置参数和命名参数，并且允许混合使用它们。

### 提取自增列值
当我们有一个包含自动生成列（通常是自增或序列）的插入语句时，我们可能想要获取生成的值。为此，我们可以使用`create()`方法，并将 isReturnGeneratedKeys 参数设置为`true`。然后，我们可以使用`result.getNewlyId()`方法来获取生成的值。

## 更新记录
testUpdate 方法展示了如何更新 shop_address 表中的现有记录：

```java
@Test
public void testUpdate() {
    // Update a record in the shop_address table
    String sql = "UPDATE shop_address SET name= '公司' WHERE id = ?";
    UpdateResult result;
    result = new Sql(conn).input(sql, 8).update();
    assertTrue(result.isOk());

    // Update a record with dynamic table name
    String sql2 = "UPDATE ${tableName} SET name= '公司' WHERE id = ?";
    result = new Sql(conn).input(sql2, mapOf("tableName", "shop_address"), 9).update();
    assertTrue(result.isOk());

    // Delete a record using update method
    String sql3 = "DELETE FROM ${tableName} WHERE id = 10"; // Delete is also update
    result = new Sql(conn).input(sql3, mapOf("tableName", "shop_address")).update();
    assertTrue(result.isOk());
}
```

## 删除记录
testDelete 方法演示了如何从 shop_address 表中删除记录：

```java
@Test
public void testDelete() {
    // Delete a record from the shop_address table
    UpdateResult result = new Sql(conn).delete("shop_address", "id", 1);
    assertTrue(result.isOk());
}
```

注意：这是物理删除，而不是逻辑删除。