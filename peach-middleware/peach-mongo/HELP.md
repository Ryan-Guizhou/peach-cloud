# peach-mongo

自定义 Mongo Starter 与自动配置，提供连接池/超时调优、模板行为控制以及可选的事务支持，且遵循 Spring Boot 条件装配原则，不覆盖用户已有配置。

## 快速开始

1. 引入依赖：

```xml
<dependency>
  <groupId>com.peach</groupId>
  <artifactId>peach-mongo-starter</artifactId>
  <version>${revision}</version>
</dependency>
```

2. 基本连接：

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://user:pass@host1:27017,host2:27017/mydb?replicaSet=rs0
```

3. 可选增强配置（peach.mongo）：

```yaml
peach:
  mongo:
    retryReads: true
    retryWrites: true
    pool:
      maxSize: 200
      minSize: 10
      maxConnecting: 50
      maxConnectionIdleTimeMs: 30000
      maintenanceInitialDelayMs: 60000
      maintenanceFrequencyMs: 15000
    socket:
      connectTimeoutMs: 8000
      readTimeoutMs: 8000
    cluster:
      serverSelectionTimeoutMs: 5000
    template:
      removeClassField: true
    transaction:
      enabled: true
```

## 属性说明

属性类见 [PeachMongoProperties.java](file:///d:/Coding/peach-cloud/peach-middleware/peach-mongo/peach-mongo-autoconfigure/src/main/java/com/peach/mongo/PeachMongoProperties.java)。

- retryReads：是否开启驱动层读重试
- retryWrites：是否开启驱动层写重试
- pool.maxSize：连接池最大连接数
- pool.minSize：连接池最小保留连接数
- pool.maxConnecting：并发建立连接的最大数
- pool.maxConnectionIdleTimeMs：连接空闲回收时间
- pool.maintenanceInitialDelayMs：维护任务初始延迟
- pool.maintenanceFrequencyMs：维护任务执行频率
- socket.connectTimeoutMs：TCP 连接建立超时
- socket.readTimeoutMs：读取数据超时
- cluster.serverSelectionTimeoutMs：选择可用服务器节点的最大等待时间
- template.removeClassField：是否移除文档中的 `_class` 字段
- transaction.enabled：是否启用事务管理器（副本集/集群要求）

## 自动配置行为

- 条件装配：
    - 未提供 `MongoDatabaseFactory` 时，基于 `spring.data.mongodb.uri` 构建工厂，并应用连接池与超时策略
    - 未提供 `MongoTemplate` 时，创建模板；当 `template.removeClassField=true` 时，移除 `_class` 字段
    - 未提供 `IMongoService` 时，装配默认通用服务 [MongoService.java](file:///d:/Coding/peach-cloud/peach-middleware/peach-mongo/peach-mongo-autoconfigure/src/main/java/com/peach/mongo/MongoService.java)
    - 当 `peach.mongo.transaction.enabled=true` 且未提供事务管理器时，装配 `MongoTransactionManager`

- 入口类：
    - [MongoAutoConfigure.java](file:///d:/Coding/peach-cloud/peach-middleware/peach-mongo/peach-mongo-autoconfigure/src/main/java/com/peach/mongo/MongoAutoConfigure.java)

## Mongo 事务说明

启用事务后会注册 `MongoTransactionManager`，可以结合 `@Transactional` 使用，实现多文档、多集合原子性：

- 适用前提：MongoDB 副本集/分片集群；单实例不支持事务
- 典型场景：需要跨集合一致性更新（例如订单主表与流水表的双写一致性）
- 使用方式：

```java
@Transactional
public void updateTwoCollections(...) {
  // 在同一数据库上下文内的多集合写操作将处于同一事务
}
```

- 注意事项：
    - 性能开销高于非事务写；仅在确有一致性需求时启用
    - 事务中读写需走同一 `MongoDatabaseFactory` 与数据库
    - 事务超时、重试策略需结合业务设置，避免长事务

## 常见问题

- 构建失败：请确保环境已安装 Maven，并使用项目父模块的版本与依赖管理；本模块代码基于 MongoDB Java Driver 4.x 的 `Duration` API。
- 用户自定义覆盖：如需自定义 `MongoTemplate` 或连接工厂，直接在业务模块提供同名 Bean 即可，Starter 不会覆盖。

