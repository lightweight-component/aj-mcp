---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---

# 与实体交互
到目前为止，我们一直在讨论使用 SQL 语句的 SqlMan。可能看起来我们离不开 SQL，但事实并非如此。SqlMan 提供了一种使用实体与数据库交互的方式，无需编写任何 SQL 代码。

这里的实体指的是 Java Bean（POJO）或 `Map` 对象。

## 查询实体

假设我们的数据库中有一个 `shop_address` 表，我们想要查询其中的所有记录。最基本的，我们需要知道要查询哪个表，所以可以这样做：

```java
List<Map<String, Object>> result2 = new Entity(conn).setTableName("shop_address").list().queryList();
assertFalse(result2.isEmpty());
```

默认情况下，它会生成以下 SQL 语句：

```sql
SELECT * FROM shop_address WHERE 1=1 ORDER BY create_date DESC
```

遵循"约定优于配置"原则，它假设表中有一个名为 `create_date` 的列。它会自动执行降序查询。

如果这样的列不存在怎么办？我们可以指定要查询的列。这完全可以，但需要更多代码：

```java
TableModel tableModel = new TableModel();
tableModel.setTableName("shop_address");
tableModel.setCreateDateField("create_at"); // 指定列名
tableModel.setHasIsDeleted(false);

List<Map<String, Object>> result = new Entity(conn).setTableModel(tableModel).list().queryList();
assertFalse(result.isEmpty());
```

`TableModel` 定义了如何查询表。

### 自定义条件查询

虽然你不能像 SQL 语句那样直接传递参数，但你仍然可以传递 `WHERE` 子句到查询中：

```java
List<Address> addresses = new Entity(conn).setTableModel(tableModel).list("AND stat = 1").queryList(Address.class);
assertNotNull(addresses);
```

记住在条件语句前添加 `AND`。

另外，这个例子展示了结果是 Java Bean 对象而不是 Map 对象。

### 使用 Java Bean 查询
使用 Java Bean 也是可以的：

```java
Address bean = new Entity(conn).setTableName("shop_address").info(1L).query(Address.class);
```

### 分页查询
分页功能的使用方式与上面相同：

```java
PageResult<Object> article = new Entity(conn).setTableName("shop_address").list().page();
assertFalse(article.isEmpty());
```

# 插入实体
这段代码演示了如何使用 SqlMan 的实体功能向数据库插入新记录。首先，创建一个新的 Address 对象并设置其属性：

```java
Address address = new Address();
address.setName("出差");
address.setAddress("广州");
address.setPhone("188");
address.setPhone2("188");
address.setRe("Tom");
```

然后，使用 `TableModel` 配置表设置：

```java
TableModel tableModel = new TableModel();
tableModel.setTableName("shop_address");
tableModel.setAutoIns(true);
tableModel.setIdTypeClz(Integer.class);
```

最后，执行插入操作：

```java
CreateResult<Integer> result = new Entity(conn).setTableModel(tableModel).input(address).create(Integer.class);
assertNotNull(result.getNewlyId());
assertTrue(result.isOk());
```

这段代码会根据 Address 对象的属性自动生成并执行 INSERT SQL 语句。`CreateResult` 对象返回操作的相关信息，包括新生成的记录 ID。

# 更新实体

这段代码演示了如何使用 SqlMan 的实体功能更新数据库中的现有记录。

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

与插入操作的主要区别是：

- `Address` 对象包含一个 ID 值（`setId(1)`），告诉 SqlMan 要更新哪条记录
- 使用 `update()` 方法而不是 `create()`
- 结果是一个 `UpdateResult` 对象，表示操作是否成功

这段代码会根据 Address 对象的属性自动生成并执行 UPDATE SQL 语句，更新 ID = 1 的记录。

## 列名映射
如果数据库中的列名与 Java Bean 中的属性名不同怎么办？SqlMan 提供了一种将列名映射到属性名的方法，只需使用 Java 注解 `@Column(name ="xxxx")` 来指定：

```java
@Data
public class Address {
    private Integer id;

    private String name;

    private String address;

    private String phone;

    @Transient // 非持久化
    private String phone2;

    @Column(name ="receiver") // 实际数据库列名是 "receiver"
    private String re;
}
```

## 非持久化字段
默认情况下，属性和字段都是持久化的。如果你不希望某个字段被更新，可以通过添加 `@Transient` 注解将该属性或字段标记为非持久化。
