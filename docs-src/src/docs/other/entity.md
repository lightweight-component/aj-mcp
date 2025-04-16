---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---
# Interact with Entity
So far, we've been discussing SqlMan with SQL statements. It might seem that we can't work without SQL, but that's not true. SqlMan provides a way to interact with databases using Entities, without writing a single line of SQL code.

Entity here, means either a Java Bean(POJO) or a `Map` object.

The Entity class in SqlMan provides a high-level abstraction for performing CRUD (Create, Read, Update, Delete) operations on database entities. It simplifies database interactions by allowing you to work with Java objects (POJOs) or Maps directly, without writing SQL statements.

## Basic Setup
There are three ways to create an Entity instance:

```java
// Using default connection
Entity entity = new Entity();

// Using specific connection
Entity entity = new Entity(connection);

// Using DataSource
Entity entity = new Entity(dataSource);
```

## Query Entity

Let's say we have a table `shop_address` in our database, and we want to query all records from it. At minimum, we need know that which table will be queried, so we can do it like this:


```java
// To fetch all records from a table:
List<Map<String, Object>> result2 = new Entity(conn).setTableName("shop_address").list().queryList();
assertFalse(result2.isEmpty());
```

By default, it will generate the following SQL statement:

```sql
SELECT * FROM shop_address WHERE 1=1 ORDER BY create_date DESC
```
Following the "Convention over Configuration" principle, it assumes there's a column named `create_date` in the table.It will automatically perform a descending order query.

What if such a column doesn't exist? We can specify which column to query. This is perfectly fine, but it requires more code:

```java
TableModel tableModel = new TableModel();
tableModel.setTableName("shop_address");
tableModel.setCreateDateField("create_at"); // specify the column name
tableModel.setHasIsDeleted(false);

List<Map<String, Object>> result = new Entity(conn).setTableModel(tableModel).list().queryList();
assertFalse(result.isEmpty());
```

`TableModel` is all about how to query a table.

### Custom condition query

Although you can't pass the parameters directly like SQL statement, you can still pass `WHERE` statement to the query:

```java
List<Address> addresses = new Entity(conn).setTableModel(tableModel).list("AND stat = 1").queryList(Address.class);
assertNotNull(addresses);
```

Remember to add `AND` before your condition statement.

Besides, this example shows the result as Java Bean object, not Map object.

### Query with Java Bean
Using a Java Bean is also possible:

```java
Address bean = new Entity(conn).setTableName("shop_address").info(1L).query(Address.class);
```
### Paging Query
The paging functionality works the same way as above:

```java
PageResult<Object> article = new Entity(conn).setTableName("shop_address").list().page();
assertFalse(article.isEmpty());
```

# Insert Entity
This code demonstrates how to insert a new record into the database using SqlMan's Entity functionality. First, it creates a new Address object and sets its properties:

```java
Address address = new Address();
address.setName("出差");
address.setAddress("广州");
address.setPhone("188");
address.setPhone2("188");
address.setRe("Tom");
```
Then, it configures the table settings using `TableModel`:

```java
TableModel tableModel = new TableModel();
tableModel.setTableName("shop_address");
tableModel.setAutoIns(true);
tableModel.setIdTypeClz(Integer.class);
```

Finally, it performs the insert operation

```java
CreateResult<Integer> result = new Entity(conn).setTableModel(tableModel).input(address).create(Integer.class);
assertNotNull(result.getNewlyId());
assertTrue(result.isOk());
```

This code will automatically generate and execute an INSERT SQL statement based on the Address object's properties. The `CreateResult` object returns information about the operation, including the newly generated ID for the inserted record.

# Update Entity

This code demonstrates how to update an existing record in the database using SqlMan's Entity functionality.

```java
Address address = new Address();
address.setId(1);
address.setName("出差");
address.setAddress("广州");
address.setPhone("188");
address.setPhone2("188");
address.setRe("Tom");

TableModel tableModel = new TableModel();
tableModel.setTableName("shop_address");
tableModel.setAutoIns(true);
tableModel.setIdTypeClz(Integer.class);

UpdateResult result;
result = new Entity(conn).setTableModel(tableModel).input(address).update();

assertTrue(result.isOk());
```
The main difference from the insert operation is:

- The `Address` object includes an ID value ( `setId(1)` ), which tells SqlMan which record to update
- It uses the `update()` method instead of `create()`
- The result is an `UpdateResult` object that indicates whether the operation succeeded
This code will automatically generate and execute an UPDATE SQL statement based on the Address object's properties, updating the record where ID = 1.


## Column name mapping
What if the column name in the database is different from the property name in the Java Bean? SqlMan provides a way to map the column name to the property name, just use a Java annotation `@Column(name ="xxxx")` to specify it:

```java
@Data
public class Address {
    private Integer id;

    private String name;

    private String address;

    private String phone;

    @Transient // not persistent
    private String phone2;

    @Column(name ="receiver") // real database column name is "receiver"
    private String re;
}
```
## Not persistent field
By default, properties and fields are persistent. If you don't want a field to be updated, you can specify that the property or field is not persistent by adding the `@Transient` annotation.
 