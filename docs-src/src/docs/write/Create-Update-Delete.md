---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---

# Updating, Creating, and Deleting Database Data
This tutorial will guide you through the process of updating, creating, and deleting data in a database using SqlMan. We'll use a sample test code to demonstrate how to perform these operations.

## Creating Records
Now that we’ve seen queries, values, and parameters, we can go back to statements and apply the same knowledge.

The `testCreate` method demonstrates how to insert new records into the `shop_address` table:

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
In the method `input()`, still, both positional and named parameters are supported, and mixing them is allowed.

### Extracting Auto-Increment Column Values
When we have an insert statement with auto-generated columns (typically auto-increment or sequences), we may want to obtain the generated values. To do this, we can use the `create()` method with the `isReturnGeneratedKeys` parameter set to `true`. Then, we can use the `result.getNewlyId()` method to retrieve the generated values.

## Updating Records

The `testUpdate` method shows how to update existing records in the `shop_address` table:

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
    System.out.println(result.getEffectedRows());

    // Delete a record using update method
    String sql3 = "DELETE FROM ${tableName} WHERE id = 10"; // Delete is also update
    result = new Sql(conn).input(sql3, mapOf("tableName", "shop_address")).update();
    assertTrue(result.isOk());
    System.out.println(result.getEffectedRows());
}
```
As we have already seen, it returns the number of affected rows.

## Deleting Records

The `testDelete` method demonstrates how to delete records from the `shop_address` table:

```java
@Test
public void testDelete() {
    // Delete a record from the shop_address table
    UpdateResult result = new Sql(conn).delete("shop_address", "id", 1);
    assertTrue(result.isOk());
}
```

Note: This is a physical delete, not a logical delete.