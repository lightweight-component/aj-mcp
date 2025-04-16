---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs-cn.njk
---

# 批量更新

SqlMan 中的 BatchUpdate 类提供了高效的批量数据库操作方法，特别适用于需要同时插入或更新多条记录的场景。

## 基本批量插入操作

### 方法一：简单批量插入
对于基本的批量插入操作，使用原始字段值：

```java
BatchUpdate batchUpdate = new BatchUpdate();
batchUpdate.setTableName("users");

// 准备数据
String fields = "name, email, age";
List<String> values = Arrays.asList(
    "('John', 'john@email.com', 25)",
    "('Jane', 'jane@email.com', 30)"
);

// 执行批量插入
batchUpdate.createBatch(fields, values);
```

### 方法二：使用 Map 对象批量插入
使用 Map 对象插入多条记录：

```java
List<Map<String, Object>> users = new ArrayList<>();

Map<String, Object> user1 = new HashMap<>();
user1.put("name", "John");
user1.put("email", "john@email.com");
users.add(user1);

Map<String, Object> user2 = new HashMap<>();
user2.put("name", "Jane");
user2.put("email", "jane@email.com");
users.add(user2);

batchUpdate.createBatchMap(users, "users");
```

### 方法三：使用 Java Bean 批量插入
使用 Java 对象插入多条记录：

```java
List<User> users = new ArrayList<>();
User user1 = new User();
user1.setName("John");
user1.setEmail("john@email.com");
users.add(user1);

User user2 = new User();
user2.setName("Jane");
user2.setEmail("jane@email.com");
users.add(user2);

batchUpdate.setTableName("users");
batchUpdate.createBatch(users);
```

## 批量删除操作
通过 ID 删除多条记录：

```java
BatchUpdate batchUpdate = new BatchUpdate();
batchUpdate.setTableName("users");
batchUpdate.setIdField("id");

List<Integer> idsToDelete = Arrays.asList(1, 2, 3, 4);
UpdateResult result = batchUpdate.deleteBatch(idsToDelete);

if (result.isOk()) {
    System.out.println("批量删除成功");
}
```

## 最佳实践

1. **事务管理**
   - 类自动处理事务
   - 批量操作期间自动禁用自动提交
   - 失败时自动回滚

2. **数据类型处理**
   该类自动处理各种数据类型：
   - 字符串会被正确引用
   - 布尔值转换为 1/0
   - 日期格式化处理
   - 支持 LocalDateTime

3. **性能提示**
   - 使用批量操作替代单条插入以提高性能
   - 保持合理的批量大小（通常为 100-1000 条记录）
   - 处理大数据集时监控内存使用

4. **错误处理**
   - 操作过程有日志记录
   - 失败时事务回滚
   - 可验证执行结果

这个 BatchUpdate 类通过减少数据库往返次数，显著提高了处理多条数据库操作时的性能。