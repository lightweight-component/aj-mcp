---
title: Query Concept
subTitle: 2024-12-05 by Frank Cheung
description: TODO
date: 2022-01-05
tags:
  - last one
layout: layouts/docs.njk
---

# Batch Update 

The BatchUpdate class in SqlMan provides efficient methods for performing batch operations on database records, particularly useful when you need to insert or update multiple records simultaneously.

## Basic Batch Insert Operations

### Method 1: Simple Batch Insert
For basic batch inserts with raw field values:

```java
BatchUpdate batchUpdate = new BatchUpdate();
batchUpdate.setTableName("users");

// Prepare your data
String fields = "name, email, age";
List<String> values = Arrays.asList(
    "('John', 'john@email.com', 25)",
    "('Jane', 'jane@email.com', 30)"
);

// Execute batch insert
batchUpdate.createBatch(fields, values);
```

### Method 2: Batch Insert with Maps
For inserting multiple records using Map objects:

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

### Method 3: Batch Insert with Java Beans
For inserting multiple records using Java objects:

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

## Batch Delete Operations
To delete multiple records by their IDs:

```java
BatchUpdate batchUpdate = new BatchUpdate();
batchUpdate.setTableName("users");
batchUpdate.setIdField("id");

List<Integer> idsToDelete = Arrays.asList(1, 2, 3, 4);
UpdateResult result = batchUpdate.deleteBatch(idsToDelete);

if (result.isOk()) {
    System.out.println("Batch delete successful");
}
```

## Best Practices

1. **Transaction Management**
   - The class handles transactions automatically
   - Auto-commit is disabled during batch operations
   - Rollback is performed automatically on failure

2. **Data Type Handling**
   The class automatically handles various data types:
   - Strings are properly quoted
   - Booleans are converted to 1/0
   - Dates are formatted appropriately
   - LocalDateTime is supported

3. **Performance Tips**
   - Use batch operations instead of individual inserts for better performance
   - Keep batch sizes reasonable (typically 100-1000 records)
   - Monitor memory usage when dealing with large datasets

4. **Error Handling**
   - Operations are logged
   - Transactions are rolled back on failure
   - Execution results are available for verification

This BatchUpdate class significantly improves performance when dealing with multiple database operations by reducing the number of database round-trips.