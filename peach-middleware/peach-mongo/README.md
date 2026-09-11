# peach-mongo

[English](README.en-US.md) | 中文

`peach-mongo` 提供 MongoDB 自动配置和统一访问入口，业务通过 `peach-mongo-starter` 接入。公开入口是自动装配的 `IMongoService`（`MongoTemplate` 封装）。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-mongo-autoconfigure` | Mongo 配置、自动装配和 `IMongoService` 契约 |
| `peach-mongo-starter` | 业务接入依赖入口 |
| `peach-mongo-quickstart` | 审计日志场景下的最小能力验证 |

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-mongo-starter</artifactId>
</dependency>
```

最小配置：`peach.mongo.uri`；URI 未带库名时再配 `peach.mongo.database`。事务默认关闭（`peach.mongo.transaction.enabled=false`），开启需要副本集或分片集群。

## Quick Start

示例位于 [`peach-mongo-quickstart`](./peach-mongo-quickstart/)，无 Web 端口（`spring.main.web-application-type=none`）。以审计日志演示 `IMongoService`。

| 项 | 说明 |
| --- | --- |
| 能力样例 | **写入查询**：`insertOne` / `findList`；**更新**：`updateOne` + `$set`；**分页删除**：`insertMany` / `findPage` / `count` / `deleteOne` |
| Runner | `MongoDemoRunner`；关闭演示：`quickstart.mongo.demo.enabled=false` |
| 测试 | Testcontainers `mongo:7.0`；无 Docker 时 `disabledWithoutDocker=true` 跳过 |
| 前置 | 本地 Mongo 或 `PEACH_MONGO_URI` |

```bash
mvn -f peach-middleware/peach-mongo/peach-mongo-quickstart/pom.xml spring-boot:run
mvn -f peach-middleware/peach-mongo/peach-mongo-quickstart/pom.xml test
```

MongoDB URI、账号和密码只通过本地配置或环境变量提供。

## 边界

- Starter 不部署 MongoDB。
- Mongo URI、账号和密码必须来自安全配置来源，不写入 README 或源码。
- 通用访问接口不替代业务聚合查询、索引和数据生命周期设计。
- `updateOne` / `updateMany` 的更新文档必须使用 `$set` 等操作符。
- 文档结构演进需要明确旧数据兼容和迁移策略。
